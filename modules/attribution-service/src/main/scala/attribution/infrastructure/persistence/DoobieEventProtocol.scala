package es.eriktorr
package attribution.infrastructure.persistence

import attribution.model.Event

import doobie.Meta
import doobie.postgres.implicits.pgEnumString

trait DoobieEventProtocol extends DoobieConversionInstanceProtocol:
  given Meta[Event.Source] =
    pgEnumString(
      "attribution_source",
      Event.Source.valueOf,
      _.toString,
    )
