package es.eriktorr
package attribution.model

import attribution.model.ConversionInstance.{ConversionAction, EventId}

import cats.derived.*
import cats.{Eq, Show}
import io.circe.Codec

final case class Attribution(
    conversionAction: ConversionAction,
    eventId: EventId,
    channel: Attribution.Channel,
    modelVersion: Attribution.ModelVersion,
) extends ConversionInstance derives Eq, Show, Codec

object Attribution:
  enum Channel derives Eq, Show, Codec:
    case Organic, PaidSearch, PaidSocial

  enum ModelVersion derives Eq, Show, Codec:
    case v1, v2
