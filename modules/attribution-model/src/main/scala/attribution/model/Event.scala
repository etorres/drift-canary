package es.eriktorr
package attribution.model

import attribution.model.ConversionInstance.{ConversionAction, EventId}

import cats.derived.*
import cats.{Eq, Show}
import io.circe.Codec
import org.typelevel.cats.time.instances.instant.given

import java.time.Instant

final case class Event(
    conversionAction: ConversionAction,
    eventId: EventId,
    userId: String,
    timestamp: Instant,
    source: Event.Source,
    amount: BigDecimal,
) extends ConversionInstance derives Eq, Show, Codec

object Event:
  enum Source derives Eq, Show, Codec:
    case Facebook, Google, Other
