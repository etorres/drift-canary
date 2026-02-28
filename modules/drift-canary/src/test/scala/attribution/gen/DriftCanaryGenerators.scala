package es.eriktorr
package attribution.gen

import attribution.client.{AttributionResult, SystemSnapshot}
import attribution.logic.AttributionLogic
import attribution.model.Attribution
import attribution.model.AttributionGenerators.{channelGen, modelVersionGen}
import attribution.model.ConversionInstance.{ConversionAction, EventId}
import attribution.model.EventGenerators.{eventGen, eventIdGen}
import attribution.util.AttributionBaseUrl
import test.gen.CollectionGenerators.{generateNDistinct, splitIntoNGroups}
import test.gen.TemporalGenerators.withinInstantRange
import test.utils.GenExtensions.sampleWithSeed

import cats.collections.Range
import cats.effect.IO
import cats.implicits.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{Method, Request}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

import java.time.{LocalDate, LocalTime, ZoneOffset}
import java.util.stream.Collectors as JCollectors
import scala.jdk.CollectionConverters.given
import scala.util.Random

@SuppressWarnings(Array("org.wartremover.warts.Any"))
trait DriftCanaryGenerators:
  final def prepareTestData(
      conversionAction: ConversionAction,
      httpClient: Client[IO],
  ): IO[Unit] =
    val systemSnapshot: SystemSnapshot =
      (for
        dates =
          val today = LocalDate.now()
          val oneWeekAgo = today.minusDays(7)
          datesBetween(oneWeekAgo, today)
        eventIds <- generateNDistinct(17 * dates.length, eventIdGen)
        dateToEventIds = dates.zip(
          splitIntoNGroups(eventIds, dates.length).map(_.toList),
        )
        eventsWithAttribution <- dateToEventIds.flatTraverse: (date, eventIds) =>
          eventsWithAttributionGen(date, conversionAction, eventIds)
        systemSnapshot = SystemSnapshot(
          events = eventsWithAttribution.map(_._1),
          attributions = eventsWithAttribution.map(_._2),
        )
      yield systemSnapshot).sampleWithSeed()
    loadSnapshot(systemSnapshot, httpClient)

  final def prepareTestData(
      conversionAction: ConversionAction,
      httpClient: Client[IO],
      attributions: List[AttributionResult],
  ): IO[Unit] =
    val systemSnapshot: SystemSnapshot =
      (for
        driftRate <- Gen
          .choose(0d, .5d)
          .map: driftRate =>
            (attributions.length * driftRate).toInt
        attributionsWithDrift =
          attributions.zipWithIndex.map: (attribution, idx) =>
            (attribution, idx < driftRate)
        eventsWithAttribution <-
          Random
            .shuffle(attributionsWithDrift)
            .traverse: (attribution, drift) =>
              eventWithAttributionGen(conversionAction, attribution, drift)
        systemSnapshot = SystemSnapshot(
          events = eventsWithAttribution.map(_._1),
          attributions = eventsWithAttribution.map(_._2),
        )
      yield systemSnapshot).sampleWithSeed()
    loadSnapshot(systemSnapshot, httpClient)

  private def datesBetween(
      start: LocalDate,
      end: LocalDate,
  ) =
    start
      .datesUntil(end.plusDays(1))
      .collect(JCollectors.toList())
      .asScala
      .toList

  private def loadSnapshot(
      systemSnapshot: SystemSnapshot,
      httpClient: Client[IO],
  ) =
    val expectedEvents = systemSnapshot.events.length
    val expectedAttributions = systemSnapshot.attributions.length
    AttributionBaseUrl.getBaseUrl.flatMap: baseUrl =>
      httpClient
        .expect(
          Request[IO](
            method = Method.POST,
            uri = baseUrl
              .addPath("admin/snapshot")
              .withQueryParam("truncate"),
          ).withEntity(systemSnapshot),
        )(using jsonOf[IO, Map[String, Int]])
        .ensureOr(response =>
          RuntimeException(
            s"Snapshot load failed: expected events_loaded=$expectedEvents, attributions_loaded=$expectedAttributions, but got $response",
          ),
        )(
          _ == Map(
            "events_loaded" -> expectedEvents,
            "attributions_loaded" -> expectedAttributions,
          ),
        )
        .void

  private def eventsWithAttributionGen(
      localDate: LocalDate,
      conversionAction: ConversionAction,
      eventIds: List[EventId],
  ) =
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
        modelVersionGen.map: modelVersion =>
          val channel = AttributionLogic.attribute(event, modelVersion)
          val attribution = Attribution(
            eventId = event.eventId,
            conversionAction = event.conversionAction,
            channel = channel,
            modelVersion = modelVersion,
          )
          event -> attribution
    yield eventsWithAttribution

  private def eventWithAttributionGen(
      conversionAction: ConversionAction,
      attributionResult: AttributionResult,
      drift: Boolean,
  ) =
    for
      event <- eventGen(
        conversionActionGen = conversionAction,
        eventIdGen = attributionResult.eventId,
      )
      (channel, modelVersion) <-
        if drift
        then
          (
            channelGen.retryUntil(_ != attributionResult.attribution.channel),
            modelVersionGen.retryUntil(_ != attributionResult.attribution.modelVersion),
          ).tupled
        else
          Gen.const(
            (
              attributionResult.attribution.channel,
              attributionResult.attribution.modelVersion,
            ),
          )
      attribution = Attribution(
        conversionAction = conversionAction,
        eventId = event.eventId,
        channel = channel,
        modelVersion = modelVersion,
      )
    yield event -> attribution
