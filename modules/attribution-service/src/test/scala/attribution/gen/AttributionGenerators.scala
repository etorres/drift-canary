package es.eriktorr
package attribution.gen

import attribution.model.AttributionGenerators.attributionGen
import attribution.model.ConversionInstance.{ConversionAction, ConversionId}
import attribution.model.ConversionInstanceGenerators.{
  conversionActionGen as defaultConversionActionGen,
  eventIdGen,
}
import attribution.model.EventGenerators.{conversionIdGen, eventGen}
import attribution.model.{Attribution, Event}
import attribution.types.{AttributionTestCase, EventTestCase}
import test.gen.CollectionGenerators.{generateNDistinct, randomlySelectOne, splitIntoThreeGroups}
import test.gen.TemporalGenerators.{instantGen, outOfInstantRange, withinInstantRange}

import cats.collections.Range
import cats.implicits.*
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.util.Random

@SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
trait AttributionGenerators:
  val findAttributionTestCaseGen: Gen[AttributionTestCase[ConversionId, Option[Attribution]]] =
    for
      size <- Gen.choose(3, 7)
      (selectedConversionId, otherConversionIds) <-
        generateNDistinct(size, conversionIdGen())
          .flatMap(_.randomlySelectOne)

      selectedEvent <- eventGenFor(selectedConversionId, instantGen)
      otherEvents <- otherConversionIds.traverse: conversionId =>
        eventGenFor(conversionId, instantGen)
      allEvents = Random.shuffle(selectedEvent :: otherEvents)

      selectedAttribution <- attributionGenFor(selectedConversionId)
      otherAttributions <- otherConversionIds.traverse: conversionId =>
        attributionGenFor(conversionId)
      allAttributions = Random.shuffle(selectedAttribution :: otherAttributions)
    yield (
      items = (allEvents, allAttributions),
      filter = selectedConversionId,
      expected = selectedAttribution.some,
    )

  val findEventTestCaseGen: Gen[EventTestCase[ConversionId, Option[Event]]] =
    for
      size <- Gen.choose(3, 7)
      (selectedConversionId, otherConversionIds) <-
        generateNDistinct(size, conversionIdGen())
          .flatMap(_.randomlySelectOne)
      selectedEvent <- eventGenFor(selectedConversionId, instantGen)
      otherEvents <- otherConversionIds.traverse: conversionId =>
        eventGenFor(conversionId, instantGen)
      allEvents = Random.shuffle(selectedEvent :: otherEvents)
    yield (
      items = allEvents,
      filter = selectedConversionId,
      expected = selectedEvent.some,
    )

  def filterEventsTestCaseGen(
      conversionActionGen: Gen[ConversionAction] = defaultConversionActionGen,
  ): Gen[EventTestCase[(ConversionAction, Range[Instant], Int), List[Event]]] =
    for
      conversionAction <- conversionActionGen
      timestampRange <- timestampRangeGen
      size <- Gen.choose(7, 11)
      eventIds <- generateNDistinct(size, eventIdGen)
      (
        selectedEventIds,
        outOfRangeEventIds,
        otherConversionActionEventIds,
      ) = eventIds.splitIntoThreeGroups
      selectedEvents <- selectedEventIds.traverse: eventId =>
        eventGen(
          eventIdGen = eventId,
          conversionActionGen = conversionAction,
          timestampGen = withinInstantRange(timestampRange),
        )
      outOfRangeEvents <- outOfRangeEventIds.traverse: eventId =>
        eventGen(
          eventIdGen = eventId,
          conversionActionGen = conversionAction,
          timestampGen = outOfInstantRange(timestampRange),
        )
      otherConversionActionEvents <- otherConversionActionEventIds.traverse: eventId =>
        eventGen(
          eventIdGen = eventId,
          conversionActionGen = defaultConversionActionGen
            .retryUntil(_ != conversionAction),
          timestampGen = outOfInstantRange(timestampRange),
        )
      allEvents = Random.shuffle(
        selectedEvents ++ outOfRangeEvents ++ otherConversionActionEvents,
      )
      dateRange = Range(
        selectedEvents
          .minByOption(_.timestamp)
          .map(_.timestamp)
          .getOrElse(Instant.MAX),
        selectedEvents
          .maxByOption(_.timestamp)
          .map(_.timestamp)
          .getOrElse(Instant.MIN),
      )
      limit = selectedEvents.length
    yield (
      items = allEvents,
      filter = (conversionAction, dateRange, limit),
      expected = selectedEvents,
    )

  private def attributionGenFor(
      conversionId: ConversionId,
  ) =
    val (conversionAction, eventId) = conversionId
    attributionGen(
      conversionActionGen = conversionAction,
      eventIdGen = eventId,
    )

  private def eventGenFor(
      conversionId: ConversionId,
      timestampGen: Gen[Instant],
  ) =
    val (conversionAction, eventId) = conversionId
    eventGen(
      conversionActionGen = conversionAction,
      eventIdGen = eventId,
      timestampGen = timestampGen,
    )

  private lazy val timestampRangeGen =
    for
      timestamp <- instantGen
      days <- Gen.choose(1, 7)
      timestampRange = Range(timestamp, timestamp.plus(days, ChronoUnit.DAYS))
    yield timestampRange
