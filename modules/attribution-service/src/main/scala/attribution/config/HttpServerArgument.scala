package es.eriktorr
package attribution.config

import attribution.config.HttpServerConfig.MaxActiveRequests

import cats.data.ValidatedNel
import cats.implicits.*
import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.Argument

trait HttpServerArgument:
  given Argument[Host] = new Argument[Host]:
    override def read(string: String): ValidatedNel[String, Host] =
      Host
        .fromString(string)
        .toRight(show"Invalid host: $string")
        .toValidatedNel
    override def defaultMetavar: String = "host"

  given Argument[MaxActiveRequests] = new Argument[MaxActiveRequests]:
    override def read(string: String): ValidatedNel[String, MaxActiveRequests] =
      string.toLongOption
        .toRight(show"A number expected, but got: $string")
        .flatMap: number =>
          MaxActiveRequests.fromNumber(number).leftMap(_.getMessage)
        .toValidatedNel
    override def defaultMetavar: String = "maxActiveRequests"

  given Argument[Port] = new Argument[Port]:
    override def read(string: String): ValidatedNel[String, Port] =
      Port
        .fromString(string)
        .toRight(show"Invalid port: $string")
        .toValidatedNel
    override def defaultMetavar: String = "port"

object HttpServerArgument extends HttpServerArgument
