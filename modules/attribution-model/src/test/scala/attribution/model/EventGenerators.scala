package es.eriktorr
package attribution.model

import attribution.model.ConversionInstance.{ConversionAction, EventId}
import attribution.model.ConversionInstanceGenerators
import test.gen.StringGenerators.alphaNumericStringBetween
import test.gen.TemporalGenerators.instantGen

import cats.implicits.*
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

import java.time.Instant
import scala.math.BigDecimal.RoundingMode

object EventGenerators extends ConversionInstanceGenerators:
  private val userIdGen = alphaNumericStringBetween(4, 12)

  private val timestampGen = instantGen

  private val sourceGen = Gen.oneOf(Event.Source.values.toSeq)

  private val amountGen = Gen
    .choose(BigDecimal(100), BigDecimal(4000))
    .map(_.setScale(4, RoundingMode.HALF_UP))

  def eventGen(
      conversionActionGen: Gen[ConversionAction] = conversionActionGen,
      eventIdGen: Gen[EventId] = eventIdGen,
      userIdGen: Gen[String] = userIdGen,
      timestampGen: Gen[Instant] = timestampGen,
      sourceGen: Gen[Event.Source] = sourceGen,
      amountGen: Gen[BigDecimal] = amountGen,
  ): Gen[Event] =
    (
      conversionActionGen,
      eventIdGen,
      userIdGen,
      timestampGen,
      sourceGen,
      amountGen,
    ).mapN(Event.apply)
