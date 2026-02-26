package es.eriktorr
package attribution.gen

import attribution.model.AttributionGenerators.attributionGen
import attribution.model.ConversionInstance.{ConversionAction, EventId}
import attribution.model.EventGenerators.eventGen
import attribution.model.{Attribution, Event}
import test.gen.TemporalGenerators.withinInstantRange

import cats.collections.Range
import cats.implicits.*
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

import java.time.{LocalDate, LocalTime, ZoneOffset}

trait DriftCanaryGenerators:
  final def eventsWithAttributionGen(
      localDate: LocalDate,
      conversionAction: ConversionAction,
      eventIds: List[EventId],
  ): Gen[List[(Event, Attribution)]] =
    for
      instantRange = Range(
        localDate.atTime(LocalTime.MIN),
        localDate.atTime(LocalTime.MAX),
      ).map(_.toInstant(ZoneOffset.UTC))
      events <- eventIds.traverse: eventId =>
        eventGen(
          conversionActionGen = conversionAction,
          eventIdGen = eventId,
          timestampGen = withinInstantRange(instantRange),
        )
      eventsWithAttribution <- events.traverse: event =>
        attributionGen(
          conversionActionGen = event.conversionAction,
          eventIdGen = event.eventId,
        ).map(event -> _)
    yield eventsWithAttribution
