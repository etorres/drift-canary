package es.eriktorr
package attribution.domain.model

import attribution.domain.model.ConversionInstance.{ConversionAction, EventId}
import attribution.domain.model.ConversionInstanceGenerators
import spec.StringGenerators.alphaNumericStringBetween
import spec.TemporalGenerators.instantGen

import cats.syntax.all.catsSyntaxTuple6Semigroupal
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

import java.time.Instant

object EventGenerators extends ConversionInstanceGenerators:
  private val userIdGen = alphaNumericStringBetween(4, 12)

  private val timestampGen = instantGen

  private val sourceGen = Gen.oneOf(Event.Source.values.toSeq)

  private val amountGen = Gen.choose(BigDecimal(100), BigDecimal(4000))

  def eventGen(
      eventIdGen: Gen[EventId] = eventIdGen,
      conversionActionGen: Gen[ConversionAction] = conversionActionGen,
      userIdGen: Gen[String] = userIdGen,
      timestampGen: Gen[Instant] = timestampGen,
      sourceGen: Gen[Event.Source] = sourceGen,
      amountGen: Gen[BigDecimal] = amountGen,
  ): Gen[Event] =
    (
      eventIdGen,
      conversionActionGen,
      userIdGen,
      timestampGen,
      sourceGen,
      amountGen,
    ).mapN(Event.apply)
