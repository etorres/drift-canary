package es.eriktorr
package attribution.infrastructure.persistence

import attribution.model.Attribution

import doobie.Meta
import doobie.postgres.implicits.pgEnumString

trait DoobieAttributionProtocol extends DoobieConversionInstanceProtocol:
  given Meta[Attribution.Channel] =
    pgEnumString(
      "attribution_channel",
      Attribution.Channel.valueOf,
      _.toString,
    )

  given Meta[Attribution.ModelVersion] =
    pgEnumString(
      "model_version",
      Attribution.ModelVersion.valueOf,
      _.toString,
    )
