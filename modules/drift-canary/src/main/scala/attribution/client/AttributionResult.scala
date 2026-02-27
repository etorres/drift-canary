package es.eriktorr
package attribution.client

import attribution.model.Attribution.{Channel, ModelVersion}
import attribution.model.ConversionInstance.{ConversionAction, EventId}

import cats.derived.*
import cats.implicits.*
import cats.{Eq, Show}
import io.circe.Codec

final case class AttributionResult(
    status: AttributionResult.Status,
    eventId: EventId,
    conversionAction: ConversionAction,
    attribution: AttributionResult.Attribution,
) derives Eq,
      Show,
      Codec:
  def isCompleted: Boolean =
    status === AttributionResult.Status.Completed

object AttributionResult:
  enum Status derives Eq, Show, Codec:
    case Accepted, Pending, Completed

  final case class Attribution(
      channel: Channel,
      modelVersion: ModelVersion,
  ) derives Eq,
        Show,
        Codec
