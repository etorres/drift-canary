package es.eriktorr
package attribution.api

import attribution.model.Attribution as AttributionModel
import attribution.model.Attribution.{Channel, ModelVersion}
import attribution.model.ConversionInstance.{ConversionAction, EventId}

import cats.derived.*
import cats.{Eq, Show}
import io.circe.Codec

final case class AttributionResult(
    status: AttributionResult.Status,
    eventId: EventId,
    conversionAction: ConversionAction,
    attribution: AttributionResult.Attribution,
) derives Eq,
      Show,
      Codec

object AttributionResult:
  enum Status derives Eq, Show, Codec:
    case Accepted, Pending, Completed

  final case class Attribution(
      channel: Channel,
      modelVersion: ModelVersion,
  ) derives Eq,
        Show,
        Codec

  def completedFrom(
      attribution: AttributionModel,
  ): AttributionResult =
    AttributionResult(
      status = Status.Completed,
      eventId = attribution.eventId,
      conversionAction = attribution.conversionAction,
      attribution = Attribution(
        channel = attribution.channel,
        modelVersion = attribution.modelVersion,
      ),
    )
