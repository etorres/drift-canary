package es.eriktorr
package attribution.model

import attribution.model.ConversionInstance.{ConversionAction, EventId}
import test.gen.StringGenerators.alphaNumericStringBetween

import org.scalacheck.Gen

trait ConversionInstanceGenerators:
  val conversionActionGen: Gen[ConversionAction] =
    alphaNumericStringBetween(4, 12)
      .map(ConversionAction.apply)

  val eventIdGen: Gen[EventId] =
    alphaNumericStringBetween(4, 12)
      .map(EventId.apply)

object ConversionInstanceGenerators extends ConversionInstanceGenerators
