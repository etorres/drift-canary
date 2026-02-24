package es.eriktorr
package attribution.model

import attribution.model.ConversionInstance.{ConversionAction, ConversionId, EventId}
import test.gen.StringGenerators.alphaNumericStringBetween

import cats.implicits.*
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

@SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
trait ConversionInstanceGenerators:
  val conversionActionGen: Gen[ConversionAction] =
    alphaNumericStringBetween(4, 12)
      .map(ConversionAction.apply)

  val eventIdGen: Gen[EventId] =
    Gen.uuid.map(EventId.apply)

  def conversionIdGen(
      conversionActionGen: Gen[ConversionAction] = conversionActionGen,
      eventIdGen: Gen[EventId] = eventIdGen,
  ): Gen[ConversionId] =
    (conversionActionGen, eventIdGen).tupled

object ConversionInstanceGenerators extends ConversionInstanceGenerators
