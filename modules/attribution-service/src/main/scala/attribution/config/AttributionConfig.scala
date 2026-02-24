package es.eriktorr
package attribution.config

import attribution.config.HealthConfig.{LivenessPath, ReadinessPath, ServiceName}
import attribution.config.HttpServerConfig.MaxActiveRequests
import attribution.config.JdbcConfig.{ConnectUrl, Password, Username}
import attribution.security.Secret

import cats.Show
import cats.collections.Range
import cats.derived.*
import cats.implicits.*
import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.Opts

import scala.concurrent.duration.FiniteDuration

final case class AttributionConfig(
    healthConfig: HealthConfig,
    httpServerConfig: HttpServerConfig,
    jdbcConfig: JdbcConfig,
) derives Show

object AttributionConfig
    extends HealthArgument
    with HttpServerArgument
    with JdbcArgument
    with RangeArgument:
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

    val jdbcConfig =
      (
        Opts
          .env[Range[Int]](
            name = "ATTRIBUTION_JDBC_CONNECTIONS",
            help = "Set JDBC connections",
          )
          .validate("Must be between 1 and 16")(_.overlaps(Range(1, 16)))
          .withDefault(Range(1, 3)),
        Opts.env[ConnectUrl](
          name = "ATTRIBUTION_JDBC_CONNECT_URL",
          help = "Set JDBC connect URL",
        ),
        Opts
          .env[Password](
            name = "ATTRIBUTION_JDBC_PASSWORD",
            help = "Set JDBC password",
          )
          .map(Secret.apply[Password]),
        Opts.env[Username](
          name = "ATTRIBUTION_JDBC_USERNAME",
          help = "Set JDBC username",
        ),
      ).mapN(JdbcConfig.apply)

    (
      healthConfig,
      httpServerConfig,
      jdbcConfig,
    ).mapN(AttributionConfig.apply)
