package es.eriktorr
package attribution.api

import attribution.api.RestController.*
import attribution.domain.model.ConversionInstance.{ConversionAction, EventId}
import attribution.domain.model.Event
import attribution.domain.service.{AttributionService, EventProcessor, EventService}

import cats.collections.Range
import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.implicits.{catsSyntaxEither, toShow}
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.dsl.io.*
import org.http4s.server.Router
import org.http4s.server.middleware.Logger as Http4sLogger
import org.http4s.{HttpApp, HttpRoutes, ParseFailure, QueryParamCodec, QueryParamDecoder, Response}
import org.typelevel.log4cats.StructuredLogger

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZoneOffset}

final class RestController(
    attributionService: AttributionService,
    eventService: EventService,
    eventProcessor: EventProcessor,
    defaultConversionAction: ConversionAction,
    enableLogger: Boolean = false,
)(using logger: StructuredLogger[IO], uuidGen: UUIDGen[IO]):
  private val apiRoutes =
    HttpRoutes.of[IO]:
      case request @ POST -> Root / "events" =>
        for
          event <- request.as[Event]
          requestId <- uuidGen.randomUUID
          _ = eventService.addIfAbsent(event)
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
          events <- eventService.filterBy(conversionAction, timestampRange, maxResults)
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
            eventService.find(conversionAction, eventId),
            attributionService.find(conversionAction, eventId),
          ).parTupled
          response <- (maybeEvent, maybeAttribution) match
            case (_, Some(attribution)) =>
              Ok(AttributionResult.completedFrom(attribution))
            case (Some(_), None) =>
              Ok(Map("status" -> AttributionResult.Status.Pending.show))
            case _ => NotFound()
        yield response

      case GET -> Root / "meta" / "model" =>
        Ok(Map("current_version" -> eventProcessor.modelVersion.show))

  private val apiEndpoint =
    if enableLogger then
      Http4sLogger.httpRoutes(
        logHeaders = true,
        logBody = true,
        redactHeadersWhen = _ => false,
        logAction = Some((msg: String) => logger.info(msg)),
      )(apiRoutes)
    else apiRoutes

  val httpApp: HttpApp[IO] =
    Router("/" -> apiEndpoint).orNotFound

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
            s"Could not parse conversion action from value: '$value'. Error: $error",
          )

  private object OptionalConversionAction
      extends OptionalQueryParamDecoderMatcher[ConversionAction]("conversion_action")
