package es.eriktorr
package common.types

import common.types.RefinedError

import cats.implicits.{catsSyntaxEither, catsSyntaxEitherId}
import io.circe.{Codec, Decoder, Encoder}

trait StringRefinements:
  extension [A <: String](self: String)
    def asNonBlank(
        fieldName: String,
    ): Either[RefinedError, String] =
      val sanitizedValue = self.trim
      if sanitizedValue.nonEmpty then sanitizedValue.asRight
      else RefinedError.EmptyOrBlankString(fieldName).asLeft

  extension [A <: String](codec: Codec.type)
    def fromRefinedString(
        builder: String => Either[RefinedError, A],
    ): Codec[A] =
      Codec.from(
        Decoder.decodeString.emap: value =>
          builder(value).leftMap(_.getMessage),
        Encoder.encodeString.contramap[A](identity),
      )
