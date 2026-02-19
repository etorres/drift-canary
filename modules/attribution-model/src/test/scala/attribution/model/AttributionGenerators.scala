package es.eriktorr
package attribution.model

import attribution.model.ConversionInstance.{ConversionAction, EventId}
import attribution.model.ConversionInstanceGenerators

import cats.syntax.apply.catsSyntaxTuple4Semigroupal
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

object AttributionGenerators extends ConversionInstanceGenerators:
  private val channelGen = Gen.oneOf(Attribution.Channel.values.toSeq)

  private val modelVersionGen = Gen.oneOf(Attribution.ModelVersion.values.toSeq)

  def attributionGen(
      eventIdGen: Gen[EventId] = eventIdGen,
      conversionActionGen: Gen[ConversionAction] = conversionActionGen,
      channelGen: Gen[Attribution.Channel] = channelGen,
      modelVersionGen: Gen[Attribution.ModelVersion] = modelVersionGen,
  ): Gen[Attribution] =
    (
      eventIdGen,
      conversionActionGen,
      channelGen,
      modelVersionGen,
    ).mapN(Attribution.apply)
