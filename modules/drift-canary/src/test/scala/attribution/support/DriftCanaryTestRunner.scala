package es.eriktorr
package attribution.support

import attribution.client.{AttributionResult, EventsFilteredByTimestamp}
import attribution.model.ConversionInstance.{ConversionAction, ConversionId, EventId}
import attribution.model.Event
import attribution.support.DriftCanaryTestRunner.{EventWithAttribution, given}
import attribution.util.AttributionBaseUrl

import cats.effect.IO
import cats.implicits.*
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{Method, QueryParamEncoder, Request, Status}
import org.typelevel.cats.time.instances.localdate.given

import java.time.LocalDate

@SuppressWarnings(Array("org.wartremover.warts.Any"))
trait DriftCanaryTestRunner extends CatsEffectSuite:
  final def selectGoldenInputEvents(
      date: LocalDate,
      conversionAction: ConversionAction,
      httpClient: Client[IO],
  ): IO[List[EventWithAttribution]] =
    for
      baseLimit = 10
      limit = (baseLimit * 1.5).toInt
      events <- getEventsBy(httpClient, conversionAction, date, limit)
      attributions <-
        events
          .traverse: event =>
            attributionFor(event, httpClient)
          .map:
            _.collect:
              case Some(value) if value.isCompleted => value
      eventsWithAttribution =
        joinByEventId(events, attributions)
          .take(baseLimit)
    yield eventsWithAttribution

  private def joinByEventId(
      events: List[Event],
      attributions: List[AttributionResult],
  ) =
    for
      event <- events
      attribution <- attributions if event.eventId === attribution.eventId
    yield (event, attribution)

  private def getEventsBy(
      httpClient: Client[IO],
      conversionAction: ConversionAction,
      date: LocalDate,
      limit: Int,
  ) =
    AttributionBaseUrl.getBaseUrl.flatMap: baseUrl =>
      httpClient
        .expect(
          Request(
            method = Method.GET,
            uri = baseUrl
              .addPath("events")
              .withQueryParam("conversion_action", conversionAction)
              .withQueryParam("from", date.toString)
              .withQueryParam("to", date.toString)
              .withQueryParam("limit", limit),
          ),
        )(using jsonOf[IO, EventsFilteredByTimestamp])
        .map(_.events)
        .ensure(RuntimeException(show"No events found for the date: $date"))(_.nonEmpty)

  private def attributionFor(
      event: Event,
      httpClient: Client[IO],
  ): IO[Option[AttributionResult]] =
    val conversionId = (event.conversionAction, event.eventId)
    attributionFor(conversionId, httpClient)

  final def replaySelectedEvents(
      conversionAction: ConversionAction,
      events: List[Event],
      httpClient: Client[IO],
  ): IO[List[Unit]] =
    events.traverse: event =>
      AttributionBaseUrl.getBaseUrl.flatMap: baseUrl =>
        httpClient
          .expect(
            Request(
              method = Method.POST,
              uri = baseUrl.addPath("events"),
            ).withEntity(event.copy(conversionAction = conversionAction)),
          )(using jsonOf[IO, Map[String, String]])
          .map(_.get("status"))
          .assertEquals("Accepted".some, "Event accepted")

  final def attributionsFor(
      conversionAction: ConversionAction,
      attributions: List[AttributionResult],
      httpClient: Client[IO],
  ): IO[List[AttributionResult]] =
    attributions
      .traverse: attribution =>
        val conversionId = (conversionAction, attribution.eventId)
        attributionFor(conversionId, httpClient)
      .map:
        _.collect:
          case Some(value) => value

  private def attributionFor(
      conversionId: ConversionId,
      httpClient: Client[IO],
  ): IO[Option[AttributionResult]] =
    val (conversionAction, eventId) = conversionId
    AttributionBaseUrl.getBaseUrl.flatMap: baseUrl =>
      httpClient
        .run(
          Request(
            method = Method.GET,
            uri = baseUrl
              .addPath(show"attributions/$eventId")
              .withQueryParam("conversion_action", conversionAction),
          ),
        )
        .use: response =>
          response.status match
            case Status.Ok =>
              response.as[AttributionResult].flatMap(IO.some)
            case Status.Accepted =>
              IO.none[AttributionResult]
            case other =>
              IO.raiseError(RuntimeException(s"Unexpected status code: ${other.code}"))

object DriftCanaryTestRunner:
  given QueryParamEncoder[ConversionAction] =
    QueryParamEncoder[String].contramap(identity)

  type EventWithAttribution = (
      event: Event,
      attribution: AttributionResult,
  )
