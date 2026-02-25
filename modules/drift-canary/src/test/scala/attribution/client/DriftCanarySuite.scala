package es.eriktorr
package attribution.client

import attribution.model.AttributionGenerators.attributionGen
import attribution.model.ConversionInstance.{ConversionAction, EventId}
import attribution.model.EventGenerators.{eventGen, eventIdGen}
import test.gen.CollectionGenerators.{generateNDistinct, splitIntoNGroups}
import test.gen.TemporalGenerators.withinInstantRange
import test.utils.GenExtensions.sampleWithSeed
import test.utils.TestFilters.{delayed, scheduled}

import cats.collections.Range
import cats.effect.{IO, Resource}
import cats.implicits.*
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Uri}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.typelevel.cats.time.instances.localdate.given

import java.time.{LocalDate, LocalTime, ZoneOffset}
import java.util.stream.Collectors as JCollectors
import scala.jdk.CollectionConverters.given

@SuppressWarnings(Array("org.wartremover.warts.Any"))
final class DriftCanarySuite extends CatsEffectSuite:
  withHttpClient()
    .test("should pick and upload selected events".tag(scheduled).only): httpClient => // TODO
      selectGoldenInputEvents(httpClient)

  private def selectGoldenInputEvents(
      httpClient: Client[IO],
  ) =
    for
      twoDaysAgo = LocalDate.now().minusDays(2)
      limit = 10
      events <- httpClient
        .expect(
          Request(
            method = Method.GET,
            uri = baseUri
              .addPath("events")
              .withQueryParam("conversion_action", "purchase_prod")
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

  withHttpClient()
    .test("should check attributions".tag(delayed)): httpClient =>
      httpClient
        .expect[String]("http://localhost:8080/hello/Ember")
        .assertEquals("123")

  private lazy val baseUri = uri"http://localhost:8080/api/v1"

  private def eventsWithAttributionGen(
      localDate: LocalDate,
      eventIds: List[EventId],
  ) =
    for
      conversionAction = ConversionAction("purchase_prod")
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

  private def prepareTestData(
      httpClient: Client[IO],
  ) =
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
        eventsWithAttribution <- dateToEventIds.flatTraverse(eventsWithAttributionGen)
        systemSnapshot = SystemSnapshot(
          events = eventsWithAttribution.map(_._1),
          attributions = eventsWithAttribution.map(_._2),
        )
      yield systemSnapshot).sampleWithSeed()
    httpClient
      .run(
        Request(
          method = Method.POST,
          uri = baseUri.addPath("admin/snapshot"),
        ).withEntity(systemSnapshot),
      )
      .use: response =>
        response
          .as[String]
          .flatMap: body =>
            IO.println(s"Status: ${response.status.code}, Body: $body")

  private def datesBetween(
      start: LocalDate,
      end: LocalDate,
  ) =
    start
      .datesUntil(end.plusDays(1))
      .collect(JCollectors.toList())
      .asScala
      .toList

  private def withHttpClient() =
    ResourceFunFixture:
      EmberClientBuilder
        .default[IO]
        .build
        .flatTap: httpClient =>
          Resource.eval(prepareTestData(httpClient))

/* TODO
Event selector & uploader
- Select from the "purchase_prod" up to 10 events uploaded two days ago, along with their attributions.
- Upload the events to the "purchase_test" conversion action.

Attributions verifier
- Get the attribution for the 10 events.
- Compare the attributions to the "purchase_prod".
 */
