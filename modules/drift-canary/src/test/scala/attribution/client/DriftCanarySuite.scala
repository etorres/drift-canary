package es.eriktorr
package attribution.client

import test.utils.TestFilters.{delayed, scheduled}

import cats.effect.IO
import cats.implicits.*
import munit.CatsEffectSuite
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.{path, uri}
import org.http4s.{Method, Request, Uri}
import org.typelevel.cats.time.instances.localdate.given

import java.time.LocalDate

@SuppressWarnings(Array("org.wartremover.warts.Any"))
final class DriftCanarySuite extends CatsEffectSuite:
  withHttpClient()
    .test("should pick and upload selected events".tag(scheduled)): httpClient =>
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
              .withPath(path"events")
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

  private lazy val baseUri = uri"http://localhost:8080"

  private def withHttpClient() =
    ResourceFunFixture:
      EmberClientBuilder
        .default[IO]
        .build

/* TODO
Event selector & uploader
- Select from the "purchase_prod" up to 10 events uploaded two days ago, along with their attributions.
- Upload the events to the "purchase_test" conversion action.

Attributions verifier
- Get the attribution for the 10 events.
- Compare the attributions to the "purchase_prod".
 */
