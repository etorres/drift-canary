package es.eriktorr
package attribution.refined.string

import attribution.refined.RefinedError
import attribution.refined.string.StringRefinements.{postgresJdbcUrlPattern, urlPathSegmentPattern}

import cats.implicits.*
import io.circe.{Codec, Decoder, Encoder}

import java.util.UUID
import scala.util.Try
import scala.util.matching.Regex

trait StringRefinements:
  extension [A <: String](self: String)
    @SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
    def asNonBlank(
        fieldName: String,
    ): Either[RefinedError, String] =
      val sanitizedValue = self.trim
      if sanitizedValue.nonEmpty then sanitizedValue.asRight
      else RefinedError.EmptyOrBlankString(fieldName).asLeft

    @SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
    def asPostgresJdbcUrl(
        fieldName: String,
    ): Either[RefinedError, String] =
      matching(
        fieldName,
        postgresJdbcUrlPattern,
        RefinedError.InvalidPostgresJdbcUrl.apply,
      )

    @SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
    def asUrlPathSegment(
        fieldName: String,
    ): Either[RefinedError, String] =
      matching(
        fieldName,
        urlPathSegmentPattern,
        RefinedError.InvalidUrlPathSegment.apply,
      )

    @SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
    def asValidUUID(
        fieldName: String,
    ): Either[RefinedError, UUID] =
      val sanitizedValue = self.trim
      Try(UUID.fromString(sanitizedValue)).toEither
        .leftMap: error =>
          RefinedError.InvalidUUID(fieldName, sanitizedValue, error)

    private def matching(
        fieldName: String,
        pattern: Regex,
        errorMaker: (String, String) => RefinedError,
    ) =
      val sanitizedValue = self.trim
      if pattern.matches(sanitizedValue) then sanitizedValue.asRight
      else errorMaker(fieldName, sanitizedValue).asLeft

  extension [A <: String](codec: Codec.type)
    @SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
    def fromRefinedString(
        builder: String => Either[RefinedError, A],
    ): Codec[A] =
      Codec.from(
        Decoder.decodeString.emap: value =>
          builder(value).leftMap(_.getMessage),
        Encoder.encodeString.contramap[A](identity),
      )

object StringRefinements:
  private val postgresJdbcUrlPattern = "jdbc:postgresql://([^:]+):(\\d+)/(\\w+)".r

  private val urlPathSegmentPattern = "^/[0-9a-zA-Z_-]+".r
