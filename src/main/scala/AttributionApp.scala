package es.eriktorr

import attribution.api.RestController
import attribution.config.{AttributionConfig, AttributionParams}
import attribution.domain.service.{AttributionService, EventProcessor, EventService}

import cats.effect.{ExitCode, IO, Resource}
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import org.http4s.ember.server.EmberServerBuilder
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
        Resource
          .eval:
            for
              logger <- Slf4jLogger.create[IO]
              given StructuredLogger[IO] = logger
              _ <- logger.info(show"Starting application with configuration: $config")
              restController <- (
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
                  enableLogger = params.verbose,
                )
            yield restController
          .flatMap: restController =>
            EmberServerBuilder
              .default[IO]
              .withHost(config.httpConfig.host)
              .withPort(config.httpConfig.port)
              .withHttpApp(restController.httpApp)
              .build
          .use(_ => IO.never)
          .as(ExitCode.Success)
