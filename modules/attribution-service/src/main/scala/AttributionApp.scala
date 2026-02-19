package es.eriktorr

import attribution.api.{HealthService, RestController}
import attribution.config.{AttributionConfig, AttributionParams, HttpServerConfig}
import attribution.service.{AttributionService, EventProcessor, EventService}

import cats.effect.{ExitCode, IO, Resource}
import cats.implicits.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{MaxActiveRequests, Timeout}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object AttributionApp
    extends CommandIOApp(
      name = "attribution-http-api",
      header = "Handle events attribution",
      version = "1.0.0",
    ):
  override def main: Opts[IO[ExitCode]] =
    (AttributionConfig.opts, AttributionParams.opts).mapN:
      case (config, params) =>
        for
          logger <- Slf4jLogger.create[IO]
          given StructuredLogger[IO] = logger
          _ <- logger.info(show"Starting application with configuration: $config")
          _ <- (for
            healthService <- HealthService.resourceWith(config.healthConfig)
            httpApp <- Resource.eval:
              restControllerFrom(healthService, params).flatMap: restController =>
                MaxActiveRequests
                  .forHttpApp[IO](config.httpServerConfig.maxActiveRequests)
                  .map: middleware =>
                    middleware(restController.httpApp)
                  .map: decoratedHttpApp =>
                    Timeout.httpApp[IO](config.httpServerConfig.timeout)(decoratedHttpApp)
            _ <- httpServerFrom(httpApp, config.httpServerConfig)
          yield healthService).use: healthService =>
            healthService.markReady >> IO.never[Unit]
        yield ExitCode.Success

  private def httpServerFrom(
      httpApp: HttpApp[IO],
      httpServerConfig: HttpServerConfig,
  ) =
    EmberServerBuilder
      .default[IO]
      .withHost(httpServerConfig.host)
      .withPort(httpServerConfig.port)
      .withHttpApp(httpApp)
      .build

  private def restControllerFrom(
      healthService: HealthService,
      params: AttributionParams,
  )(using logger: StructuredLogger[IO]) =
    (
      AttributionService.inMemory,
      EventService.inMemory,
    ).mapN: (attributionService, eventService) =>
      RestController(
        attributionService = attributionService,
        eventService = eventService,
        eventProcessor = EventProcessor(
          attributionService,
          params.modelVersion,
        ),
        defaultConversionAction = params.defaultConversionAction,
        healthService = healthService,
        enableLogger = params.verbose,
      )
