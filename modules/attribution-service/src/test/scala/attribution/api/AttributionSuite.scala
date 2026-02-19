package es.eriktorr
package attribution.api

import attribution.api.HealthServiceStub.HealthServiceState
import attribution.api.RestController
import attribution.model.Attribution.ModelVersion
import attribution.model.AttributionGenerators.attributionGen
import attribution.model.ConversionInstance.ConversionAction
import attribution.model.ConversionInstanceGenerators.eventIdGen
import attribution.model.Event
import attribution.model.EventGenerators.{conversionActionGen, eventGen}
import attribution.service.{AttributionService, EventProcessor, EventService}
import test.gen.CollectionGenerators.{generateNDistinct, splitIntoThreeGroups}
import test.gen.TemporalGenerators.{instantGen, outOfInstantRange, withinInstantRange}
import test.infrastructure.DeterministicUUIDGen
import test.infrastructure.DeterministicUUIDGen.UUIDGenState

import cats.collections.Range
import cats.effect.std.UUIDGen
import cats.effect.{IO, Ref, Resource}
import cats.implicits.*
import fs2.Stream
import io.circe.Encoder
import io.circe.syntax.given
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.implicits.uri
import org.http4s.{EntityDecoder, Method, Request, Response, Status, Uri}
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllNoShrinkF
import org.scalacheck.{Gen, Test}
import org.typelevel.cats.time.instances.localdate.given
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.nio.charset.StandardCharsets
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, ZoneId}
import scala.util.Random

@SuppressWarnings(Array("org.wartremover.warts.Any"))
final class AttributionSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  withHttpApp(ModelVersion.v1, testConversionAction)
    .test("should register a new event"): (_, _, _, uuidGenStateRef, httpApp) =>
      forAllNoShrinkF((eventGen(), Gen.uuid).tupled): (event, uuid) =>
        for
          _ <- uuidGenStateRef.update(_.one(uuid))
          response <- httpApp.run(
            Request(
              method = Method.POST,
              uri = uri"/api/v1/events",
              body = requestBodyFrom(event),
            ),
          )
          _ <- check(
            actualResponse = response,
            expectedStatus = Status.Ok,
            expectedBody = Map(
              "request_id" -> uuid.toString,
              "status" -> AttributionResult.Status.Accepted.show,
            ).some,
          )
        yield ()

  withHttpApp(ModelVersion.v1, testConversionAction)
    .test("should filter events by timestamp"): (_, _, eventService, _, httpApp) =>
      val timestampRangeGen =
        for
          timestamp <- instantGen
          days <- Gen.choose(1, 7)
          timestampRange = Range(timestamp, timestamp.plus(days, ChronoUnit.DAYS))
        yield timestampRange
      val testCaseGen =
        for
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
              conversionActionGen = testConversionAction,
              timestampGen = withinInstantRange(timestampRange),
            )
          outOfRangeEvents <- outOfRangeEventIds.traverse: eventId =>
            eventGen(
              eventIdGen = eventId,
              conversionActionGen = testConversionAction,
              timestampGen = outOfInstantRange(timestampRange),
            )
          otherConversionActionEvents <- otherConversionActionEventIds.traverse: eventId =>
            eventGen(
              eventIdGen = eventId,
              conversionActionGen = conversionActionGen
                .retryUntil(_ != testConversionAction),
              timestampGen = outOfInstantRange(timestampRange),
            )
          allEvents = Random.shuffle(
            selectedEvents ++ outOfRangeEvents ++ otherConversionActionEvents,
          )
        yield allEvents -> selectedEvents
      forAllNoShrinkF(testCaseGen): (allEvents, selectedEvents) =>
        for
          _ <- allEvents.parTraverse_(eventService.addIfAbsent)
          minDate = selectedEvents
            .minByOption(_.timestamp)
            .map(_.timestamp.atZone(ZoneId.of("UTC")).toLocalDate)
            .getOrElse(LocalDate.MAX)
          maxDate = selectedEvents
            .maxByOption(_.timestamp)
            .map(_.timestamp.atZone(ZoneId.of("UTC")).toLocalDate)
            .getOrElse(LocalDate.MIN)
          limit = selectedEvents.length
          response <- httpApp.run(
            Request(
              method = Method.GET,
              uri =
                Uri.unsafeFromString(show"/api/v1/events?from=$minDate&to=$maxDate&limit=$limit"),
            ),
          )
          _ <- check(
            actualResponse = response,
            expectedStatus = Status.Ok,
            expectedBody = EventsFilteredByTimestamp(
              from = minDate,
              to = maxDate,
              limit = limit.some,
              events = selectedEvents.sortBy(_.eventId),
            ).some,
          )
        yield ()

  withHttpApp(ModelVersion.v1, testConversionAction)
    .test("should find an assigned attribution"):
      (_, attributionService, eventService, _, httpApp) =>
        val testCaseGen =
          for
            event <- eventGen(
              conversionActionGen = testConversionAction,
            )
            attribution <- attributionGen(
              conversionActionGen = testConversionAction,
              eventIdGen = event.eventId,
            )
          yield event -> attribution
        forAllNoShrinkF(testCaseGen): (event, attribution) =>
          for
            _ <- attributionService.addIfAbsent(attribution)
            _ <- eventService.addIfAbsent(event)
            response <- httpApp.run(
              Request(
                method = Method.GET,
                uri = Uri.unsafeFromString(show"/api/v1/attributions/${event.eventId}"),
              ),
            )
            _ <- check(
              actualResponse = response,
              expectedStatus = Status.Ok,
              expectedBody = AttributionResult.completedFrom(attribution).some,
            )
          yield ()

  withHttpApp(ModelVersion.v1, testConversionAction)
    .test("should find a pending attribution"): (_, _, eventService, _, httpApp) =>
      forAllNoShrinkF(eventGen(conversionActionGen = testConversionAction)): event =>
        for
          _ <- eventService.addIfAbsent(event)
          response <- httpApp.run(
            Request(
              method = Method.GET,
              uri = Uri.unsafeFromString(show"/api/v1/attributions/${event.eventId}"),
            ),
          )
          _ <- check(
            actualResponse = response,
            expectedStatus = Status.Ok,
            expectedBody = Map("status" -> AttributionResult.Status.Pending.show).some,
          )
        yield ()

  withHttpApp(ModelVersion.v1, testConversionAction)
    .test("should fail with a not found error when the event is unregistered"):
      (_, _, _, _, httpApp) =>
        forAllNoShrinkF(eventIdGen): eventId =>
          httpApp
            .run(
              Request(
                method = Method.GET,
                uri = Uri.unsafeFromString(show"/api/v1/attributions/$eventId"),
              ),
            )
            .flatMap: response =>
              check(
                actualResponse = response,
                expectedStatus = Status.NotFound,
                expectedBody = Option.empty[String],
              )

  withHttpApp(ModelVersion.v1, testConversionAction)
    .test("should get the current model version"): (modelVersion, _, _, _, httpApp) =>
      httpApp
        .run(
          Request(
            method = Method.GET,
            uri = uri"/api/v1/meta/model",
          ),
        )
        .flatMap: response =>
          check(
            actualResponse = response,
            expectedStatus = Status.Ok,
            expectedBody = Map("current_version" -> modelVersion.show).some,
          )

  private def check[A](
      actualResponse: Response[IO],
      expectedStatus: Status,
      expectedBody: Option[A],
  )(using ev: EntityDecoder[IO, A]) =
    for
      actualStatus = actualResponse.status
      _ <- assertIO(
        IO.pure(actualStatus),
        expectedStatus,
        "Response status",
      )
      actualBody <- (actualStatus match
        case Status.Ok => actualResponse.as[A].map(_.some)
        case _ => IO(Option.empty[A])
      ).assertEquals(expectedBody, "Response body")
    yield ()

  private def requestBodyFrom[A: Encoder](
      value: A,
  ) =
    Stream
      .emits(value.asJson.noSpaces.getBytes(StandardCharsets.UTF_8))
      .covary[IO]

  private def withHttpApp(
      modelVersion: ModelVersion,
      defaultConversionAction: ConversionAction,
  ) =
    ResourceFunFixture:
      Resource.eval:
        for
          logger <- Slf4jLogger.create[IO]
          given StructuredLogger[IO] = logger
          attributionService <- AttributionService.inMemory
          eventService <- EventService.inMemory
          healthServiceStatRef <- Ref.of[IO, HealthServiceState](
            HealthServiceState.unready,
          )
          uuidGenStateRef <- Ref.of[IO, UUIDGenState](
            UUIDGenState.empty,
          )
          given UUIDGen[IO] = DeterministicUUIDGen(uuidGenStateRef)
          httpApp = RestController(
            attributionService = attributionService,
            eventService = eventService,
            eventProcessor = EventProcessor(
              attributionService,
              modelVersion,
            ),
            defaultConversionAction = defaultConversionAction,
            healthService = HealthServiceStub(healthServiceStatRef),
          ).httpApp
        yield (
          modelVersion,
          attributionService,
          eventService,
          uuidGenStateRef,
          httpApp,
        )

  private lazy val testConversionAction = ConversionAction("purchase_test")

  override def scalaCheckTestParameters: Test.Parameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(1)
