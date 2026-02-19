package es.eriktorr
package attribution.config

import attribution.config.HealthConfig.{LivenessPath, ReadinessPath, ServiceName}
import attribution.config.HttpServerConfig.MaxActiveRequests

import cats.Show
import cats.derived.*
import cats.implicits.*
import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.Opts

import scala.concurrent.duration.FiniteDuration

final case class AttributionConfig(
    healthConfig: HealthConfig,
    httpServerConfig: HttpServerConfig,
) derives Show

object AttributionConfig extends HealthArgument with HttpServerArgument:
  def opts: Opts[AttributionConfig] =
    val healthConfig =
      (
        Opts
          .env[LivenessPath](
            name = "ATTRIBUTION_HEALTH_LIVENESS_PATH",
            help = "Set liveness path",
          )
          .withDefault(HealthConfig.defaultLivenessPath),
        Opts
          .env[ReadinessPath](
            name = "ATTRIBUTION_HEALTH_READINESS_PATH",
            help = "Set readiness path",
          )
          .withDefault(HealthConfig.defaultReadinessPath),
        Opts
          .env[ServiceName](
            name = "ATTRIBUTION_HEALTH_SERVICE_NAME",
            help = "Set service name",
          )
          .withDefault(ServiceName("Attribution Service")),
      ).mapN(HealthConfig.apply)

    val httpServerConfig =
      (
        Opts
          .env[Host](
            name = "ATTRIBUTION_HTTP_SERVER_HOST",
            help = "Set the HTTP server host",
          )
          .withDefault(HttpServerConfig.defaultHost),
        Opts
          .env[MaxActiveRequests](
            name = "ATTRIBUTION_HTTP_SERVER_MAX_ACTIVE_REQUESTS",
            help = "Set HTTP server max active requests",
          )
          .withDefault(HttpServerConfig.defaultMaxActiveRequests),
        Opts
          .env[Port](
            name = "ATTRIBUTION_HTTP_SERVER_PORT",
            help = "Set the HTTP server port",
          )
          .withDefault(HttpServerConfig.defaultPort),
        Opts
          .env[FiniteDuration](
            name = "ATTRIBUTION_HTTP_SERVER_TIMEOUT",
            help = "Set HTTP server timeout",
          )
          .withDefault(HttpServerConfig.defaultTimeout),
      ).mapN(HttpServerConfig.apply)

    (healthConfig, httpServerConfig).mapN(AttributionConfig.apply)
