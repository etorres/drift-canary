package es.eriktorr
package attribution.logic

import attribution.model.Attribution.{Channel, ModelVersion}
import attribution.model.Event
import attribution.model.Event.Source

object AttributionLogic:
  def attribute(
      event: Event,
      modelVersion: ModelVersion,
  ): Channel =
    modelVersion match
      case ModelVersion.v1 =>
        event.source match
          case Source.Facebook => Channel.PaidSocial
          case Source.Google => Channel.PaidSearch
          case Source.Other => Channel.Organic
      case ModelVersion.v2 =>
        event.source match
          case Source.Facebook => Channel.PaidSocial
          case Source.Google => Channel.Organic // regression
          case Source.Other => Channel.Organic
