package es.eriktorr
package attribution.model

import attribution.model.ConversionInstance.{ConversionAction, EventId}
import attribution.model.ConversionInstanceGenerators

import cats.syntax.apply.catsSyntaxTuple4Semigroupal
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

object AttributionGenerators extends ConversionInstanceGenerators:
  private val channelGen = Gen.oneOf(Attribution.Channel.values.toSeq)

  val modelVersionGen: Gen[Attribution.ModelVersion] =
    Gen.oneOf(Attribution.ModelVersion.values.toSeq)

  def attributionGen(
      conversionActionGen: Gen[ConversionAction] = conversionActionGen,
      eventIdGen: Gen[EventId] = eventIdGen,
      channelGen: Gen[Attribution.Channel] = channelGen,
      modelVersionGen: Gen[Attribution.ModelVersion] = modelVersionGen,
  ): Gen[Attribution] =
    (
      conversionActionGen,
      eventIdGen,
      channelGen,
      modelVersionGen,
    ).mapN(Attribution.apply)
