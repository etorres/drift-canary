package es.eriktorr
package attribution.refined.string

import attribution.refined.RefinedError
import attribution.refined.string.StringRefinements.{urlPathSegmentPattern, uuidPattern}

import cats.implicits.*
import io.circe.{Codec, Decoder, Encoder}

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
    def asUrlPathSegment(
        fieldName: String,
    ): Either[RefinedError, String] =
      val sanitizedValue = self.trim
      if urlPathSegmentPattern.matches(sanitizedValue) then sanitizedValue.asRight
      else RefinedError.InvalidUrlPathSegment(fieldName, sanitizedValue).asLeft

    @SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
    def asValidUUID(
        fieldName: String,
    ): Either[RefinedError, String] =
      val sanitizedValue = self.trim
      if uuidPattern.matches(sanitizedValue) then sanitizedValue.asRight
      else RefinedError.InvalidUUID(fieldName, sanitizedValue).asLeft

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
  private val urlPathSegmentPattern = "^/[0-9a-zA-Z_-]+".r

  private val uuidPattern =
    "^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})".r
