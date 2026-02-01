package es.eriktorr
package attribution.config

import cats.Show
import cats.derived.*
import cats.implicits.catsSyntaxTuple2Semigroupal
import com.comcast.ip4s.{host, port, Host, Port}
import com.monovore.decline.Opts

final case class AttributionConfig(
    httpConfig: AttributionConfig.HttpConfig,
) derives Show

object AttributionConfig extends Ip4sArgument:
  def opts: Opts[AttributionConfig] =
    val httpOpts =
      (
        Opts
          .env[Host](
            name = "ATTRIBUTION_HTTP_HOST",
            help = "Set the HTTP host",
          )
          .withDefault(host"localhost"),
        Opts
          .env[Port](
            name = "ATTRIBUTION_HTTP_PORT",
            help = "Set the HTTP port",
          )
          .withDefault(port"8080"),
      ).mapN(HttpConfig.apply)
    httpOpts.map(AttributionConfig.apply)

  final case class HttpConfig(
      host: Host,
      port: Port,
  )
