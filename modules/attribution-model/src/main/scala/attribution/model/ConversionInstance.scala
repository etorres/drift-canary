package es.eriktorr
package attribution.model

import attribution.refined.Refined.{asNonBlank, asValidUUID, fromRefinedString, unsafeFrom}
import attribution.refined.RefinedError

import cats.implicits.*
import cats.{Eq, Show}
import io.circe.Codec

trait ConversionInstance:
  val conversionAction: ConversionInstance.ConversionAction
  val eventId: ConversionInstance.EventId

  final inline def conversionInstancePath: String =
    ConversionInstance.conversionInstancePath(conversionAction, eventId)

  final inline def conversionInstanceAsString: String =
    show"conversionAction=$conversionAction, eventId=$eventId"

object ConversionInstance:
  def conversionInstancePath(
      conversionId: ConversionId,
  ): String =
    val (conversionAction, eventId) = conversionId
    show"$conversionAction/$eventId"

  type ConversionId = (ConversionAction, EventId)

  opaque type ConversionAction <: String = String

  object ConversionAction:
    def fromString(
        value: String,
    ): Either[RefinedError, ConversionAction] =
      value.asNonBlank("ConversionAction")

    def apply(value: String): ConversionAction =
      value.unsafeFrom(ConversionAction.fromString)

    given Eq[ConversionAction] = Eq.fromUniversalEquals

    given Codec[ConversionAction] = Codec.fromRefinedString(ConversionAction.fromString)
  end ConversionAction

  opaque type EventId <: String = String

  object EventId:
    def fromString(
        value: String,
    ): Either[RefinedError, EventId] =
      value.asValidUUID("EventId")

    def apply(value: String): EventId =
      value.unsafeFrom(EventId.fromString)

    given Eq[EventId] = Eq.fromUniversalEquals

    given Codec[EventId] = Codec.fromRefinedString(EventId.fromString)
  end EventId
