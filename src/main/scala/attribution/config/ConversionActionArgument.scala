package es.eriktorr
package attribution.config

import attribution.domain.model.ConversionInstance.ConversionAction

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxEither
import com.monovore.decline.Argument

trait ConversionActionArgument:
  given conversionActionArgument: Argument[ConversionAction] =
    new Argument[ConversionAction]:
      override def read(string: String): ValidatedNel[String, ConversionAction] =
        ConversionAction
          .fromString(string)
          .leftMap(_.getMessage)
          .toValidatedNel

      override def defaultMetavar: String = "conversionAction"

object ConversionActionArgument extends ConversionActionArgument
