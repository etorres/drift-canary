package es.eriktorr
package attribution.config

import cats.data.ValidatedNel
import cats.syntax.validated.catsSyntaxValidatedId
import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.Argument

trait Ip4sArgument:
  given hostArgument: Argument[Host] =
    new Argument[Host]:
      override def read(string: String): ValidatedNel[String, Host] =
        Host.fromString(string) match
          case Some(value) => value.validNel
          case None => s"Invalid host: $string".invalidNel

      override def defaultMetavar: String = "host"

  given portArgument: Argument[Port] =
    new Argument[Port]:
      override def read(string: String): ValidatedNel[String, Port] = Port.fromString(string) match
        case Some(value) => value.validNel
        case None => s"Invalid port: $string".invalidNel

      override def defaultMetavar: String = "port"

object Ip4sArgument extends Ip4sArgument
