package es.eriktorr
package attribution.api

import attribution.api.RestController.*
import attribution.domain.service.{AttributionService, EventProcessor, EventService}
import attribution.model.ConversionInstance.{ConversionAction, EventId}
import attribution.model.Event

import cats.collections.Range
import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.implicits.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*
import org.http4s.headers.`Retry-After`
import org.http4s.server.Router
import org.http4s.server.middleware.Logger as Http4sLogger
import org.http4s.{HttpApp, HttpRoutes, ParseFailure, QueryParamCodec, QueryParamDecoder, Response}
import org.typelevel.log4cats.StructuredLogger

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZoneOffset}
import scala.concurrent.duration.DurationInt

final class RestController(
    attributionService: AttributionService,
    eventService: EventService,
    eventProcessor: EventProcessor,
    defaultConversionAction: ConversionAction,
    healthService: HealthService,
    enableLogger: Boolean = false,
)(using logger: StructuredLogger[IO], uuidGen: UUIDGen[IO]):
  private val apiRoutes =
    HttpRoutes.of[IO]:
      case request @ POST -> Root / "events" =>
        for
          event <- request.as[Event]
          requestId <- uuidGen.randomUUID
          _ = eventService.record(event)
          _ <- eventProcessor.process(event)
          response <- Ok(
            Map(
              "request_id" -> requestId.toString,
              "status" -> AttributionResult.Status.Accepted.show,
            ),
          )
        yield response

      case GET -> Root / "events" :? FromDate(fromDate) +& ToDate(toDate) +& Limit(maybeLimit)
          +& OptionalConversionAction(maybeConversionAction) =>
        for
          timestampRange = Range(
            fromDate.atTime(LocalTime.MIN).toInstant(ZoneOffset.UTC),
            toDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC),
          )
          maxResults = maybeLimit.getOrElse(10)
          conversionAction = maybeConversionAction.getOrElse(defaultConversionAction)
          events <- eventService.query(conversionAction, timestampRange, maxResults)
          response <- Ok(
            EventsFilteredByTimestamp(
              from = fromDate,
              to = toDate,
              limit = maybeLimit,
              events = events.sortBy(_.eventId),
            ),
          )
        yield response

      case GET -> Root / "attributions" / EventIdVar(eventId)
          :? OptionalConversionAction(maybeConversionAction) =>
        for
          conversionAction = maybeConversionAction.getOrElse(defaultConversionAction)
          (maybeEvent, maybeAttribution) <- (
            eventService.get(conversionAction, eventId),
            attributionService.get(conversionAction, eventId),
          ).parTupled
          response <- (maybeEvent, maybeAttribution) match
            case (_, Some(attribution)) =>
              Ok(AttributionResult.completedFrom(attribution))
            case (Some(_), None) =>
              Accepted(
                Map("status" -> AttributionResult.Status.Pending.show),
                `Retry-After`.unsafeFromDuration(60.seconds),
              )
            case _ => NotFound()
        yield response

      case request @ POST -> Root / "admin" / "snapshot" =>
        for
          snapshot <- request.as[SystemSnapshot]
          insertedEvents <- snapshot.events.traverse(eventService.record)
          insertedAttributions <- snapshot.attributions.traverse(attributionService.record)
          response <- Ok(
            Map(
              "events_loaded" -> insertedEvents.length,
              "attributions_loaded" -> insertedAttributions.length,
            ),
          )
        yield response

      case GET -> Root / "meta" / "model" =>
        Ok(Map("current_version" -> eventProcessor.modelVersion.show))

  private val apiEndpoint =
    if enableLogger then
      Http4sLogger.httpRoutes(
        logHeaders = true,
        logBody = true,
        redactHeadersWhen = _ => false,
        logAction = ((msg: String) => logger.info(msg)).some,
      )(apiRoutes)
    else apiRoutes

  private val livenessCheckEndpoint =
    HttpRoutes.of[IO]:
      case GET -> Root =>
        Ok(show"${healthService.serviceName} is live")

  private val readinessCheckEndpoint =
    HttpRoutes.of[IO]:
      case GET -> Root =>
        healthService.isReady.ifM(
          ifTrue = Ok(show"${healthService.serviceName} is ready"),
          ifFalse = ServiceUnavailable(show"${healthService.serviceName} is not ready"),
        )

  val httpApp: HttpApp[IO] =
    Router(
      "/api/v1" -> apiEndpoint,
      healthService.livenessPath -> livenessCheckEndpoint,
      healthService.readinessPath -> readinessCheckEndpoint,
    ).orNotFound

object RestController:
  private object EventIdVar:
    def unapply(value: String): Option[EventId] =
      EventId.fromString(value).toOption

  private given QueryParamCodec[LocalDate] =
    QueryParamCodec.localDate(DateTimeFormatter.ISO_LOCAL_DATE)

  private object FromDate extends QueryParamDecoderMatcher[LocalDate]("from")

  private object ToDate extends QueryParamDecoderMatcher[LocalDate]("to")

  private object Limit extends OptionalQueryParamDecoderMatcher[Int]("limit")

  private given QueryParamDecoder[ConversionAction] =
    QueryParamDecoder[String].emap: value =>
      ConversionAction
        .fromString(value)
        .leftMap: error =>
          ParseFailure(
            "Invalid conversion action",
            show"Could not parse conversion action from value: '$value'. Error: $error",
          )

  private object OptionalConversionAction
      extends OptionalQueryParamDecoderMatcher[ConversionAction]("conversion_action")
