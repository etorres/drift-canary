package es.eriktorr
package attribution.support

import attribution.client.{EventsFilteredByTimestamp, SystemSnapshot}
import attribution.gen.DriftCanaryGenerators
import attribution.model.ConversionInstance.ConversionAction
import attribution.model.Event
import attribution.model.EventGenerators.eventIdGen
import attribution.support.DriftCanaryTestRunner.given
import test.gen.CollectionGenerators.{generateNDistinct, splitIntoNGroups}
import test.utils.GenExtensions.sampleWithSeed

import cats.effect.IO
import cats.implicits.*
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.http4s.{Method, QueryParamEncoder, Request, Uri}
import org.scalacheck.cats.implicits.given
import org.typelevel.cats.time.instances.localdate.given

import java.time.LocalDate
import java.util.stream.Collectors as JCollectors
import scala.jdk.CollectionConverters.given

@SuppressWarnings(Array("org.wartremover.warts.Any"))
trait DriftCanaryTestRunner extends CatsEffectSuite with DriftCanaryGenerators:
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
    httpClient
      .expect(
        Request(
          method = Method.POST,
          uri = baseUri.addPath("admin/snapshot"),
        ).withEntity(systemSnapshot),
      )(using jsonOf[IO, Map[String, Int]])
      .assertEquals(
        Map(
          "events_loaded" -> systemSnapshot.events.length,
          "attributions_loaded" -> systemSnapshot.attributions.length,
        ),
      )

  final def selectGoldenInputEvents(
      httpClient: Client[IO],
      conversionAction: ConversionAction,
  ): IO[List[Event]] =
    for
      twoDaysAgo = LocalDate.now().minusDays(2)
      limit = 10
      events <- httpClient
        .expect(
          Request(
            method = Method.GET,
            uri = baseUri
              .addPath("events")
              .withQueryParam("conversion_action", conversionAction)
              .withQueryParam("from", twoDaysAgo.toString)
              .withQueryParam("to", twoDaysAgo.toString)
              .withQueryParam("limit", limit),
          ),
        )(using jsonOf[IO, EventsFilteredByTimestamp])
        .map(_.events)
      _ <- assertIOBoolean(
        IO.pure(events.nonEmpty),
        show"No events found for the date: $twoDaysAgo",
      )
    yield events

  final def replaySelectedEvents(
      httpClient: Client[IO],
      conversionAction: ConversionAction,
      events: List[Event],
  ): IO[List[Unit]] =
    events.traverse: event =>
      httpClient
        .expect(
          Request(
            method = Method.POST,
            uri = baseUri.addPath("events"),
          ).withEntity(event.copy(conversionAction = conversionAction)),
        )(using jsonOf[IO, Map[String, String]])
        .map(_.get("status"))
        .assertEquals("Accepted".some, "Event accepted")

  private def datesBetween(
      start: LocalDate,
      end: LocalDate,
  ) =
    start
      .datesUntil(end.plusDays(1))
      .collect(JCollectors.toList())
      .asScala
      .toList

  private lazy val baseUri = uri"http://localhost:8080/api/v1"

object DriftCanaryTestRunner:
  given QueryParamEncoder[ConversionAction] =
    QueryParamEncoder[String].contramap(identity)
