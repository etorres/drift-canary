package es.eriktorr
package attribution.config

import attribution.config.JdbcConfig.{ConnectUrl, Password, Username}
import attribution.refined.Refined.{asNonBlank, asPostgresJdbcUrl, unsafeFrom}
import attribution.refined.RefinedError
import attribution.security.Secret

import cats.Show
import cats.collections.Range
import cats.derived.*
import cats.implicits.*

final case class JdbcConfig(
    connections: Range[Int],
    connectUrl: ConnectUrl,
    password: Secret[Password],
    username: Username,
) derives Show

object JdbcConfig:
  opaque type ConnectUrl <: String = String

  object ConnectUrl:
    def fromString(
        value: String,
    ): Either[RefinedError, ConnectUrl] =
      value.asPostgresJdbcUrl("ConnectUrl")

    def apply(value: String): ConnectUrl =
      value.unsafeFrom(ConnectUrl.fromString)
  end ConnectUrl

  opaque type Password <: String = String

  object Password:
    def fromString(
        value: String,
    ): Either[RefinedError, Password] =
      value.asNonBlank("Password")

    def apply(value: String): Password =
      value.unsafeFrom(Password.fromString)

    given Show[Password] = Show.fromToString
  end Password

  opaque type Username <: String = String

  object Username:
    def fromString(
        value: String,
    ): Either[RefinedError, Username] =
      value.asNonBlank("Username")

    def apply(value: String): Username =
      value.unsafeFrom(Username.fromString)
  end Username
