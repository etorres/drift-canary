package es.eriktorr
package attribution.config

import attribution.config.JdbcConfig.{ConnectUrl, Password, Username}

import cats.data.ValidatedNel
import cats.implicits.*
import com.monovore.decline.Argument

trait JdbcArgument:
  given connectUrlArgument: Argument[ConnectUrl] = new Argument[ConnectUrl]:
    override def read(string: String): ValidatedNel[String, ConnectUrl] =
      ConnectUrl
        .fromString(string)
        .leftMap(_.getMessage)
        .toValidatedNel
    override def defaultMetavar: String = "jdbcUrl"

  given passwordArgument: Argument[Password] = new Argument[Password]:
    override def read(string: String): ValidatedNel[String, Password] =
      Password
        .fromString(string)
        .leftMap(_.getMessage)
        .toValidatedNel
    override def defaultMetavar: String = "password"

  given usernameArgument: Argument[Username] = new Argument[Username]:
    override def read(string: String): ValidatedNel[String, Username] =
      Username
        .fromString(string)
        .leftMap(_.getMessage)
        .toValidatedNel
    override def defaultMetavar: String = "username"

object JdbcArgument extends JdbcArgument
