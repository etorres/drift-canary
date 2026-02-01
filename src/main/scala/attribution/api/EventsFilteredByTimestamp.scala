package es.eriktorr
package attribution.api

import attribution.domain.model.Event

import cats.derived.*
import cats.{Eq, Show}
import io.circe.Codec
import org.typelevel.cats.time.instances.localdate.given

import java.time.LocalDate

final case class EventsFilteredByTimestamp(
    from: LocalDate,
    to: LocalDate,
    limit: Option[Int],
    events: List[Event],
) derives Eq,
      Show,
      Codec
