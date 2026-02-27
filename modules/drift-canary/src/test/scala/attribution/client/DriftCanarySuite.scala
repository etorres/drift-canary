package es.eriktorr
package attribution.client

import attribution.client.DriftCanarySuite.*
import attribution.gen.DriftCanaryGenerators
import attribution.health.{HistoryManager, PipelineHealthDashboard, VerificationResult}
import attribution.infrastructure.{ArtifactReader, ArtifactStore, ArtifactWriter}
import attribution.model.ConversionInstance.ConversionAction
import attribution.support.DriftCanaryTestRunner
import test.utils.TestFilters.{delayed, scheduled}

import cats.effect.{IO, Resource}
import cats.implicits.*
import munit.CatsEffectSuite
import org.http4s.ember.client.EmberClientBuilder

import java.time.LocalDate

@SuppressWarnings(Array("org.wartremover.warts.Any"))
final class DriftCanarySuite extends DriftCanaryTestRunner with DriftCanaryGenerators:
  withHttpClient(Stage.Uploader)
    .test("should pick and upload selected events".tag(scheduled)): httpClient =>
      (for
        twoDaysAgo = LocalDate.now().minusDays(2)
        (events, attributions) <-
          selectGoldenInputEvents(twoDaysAgo, prodConversionAction, httpClient).map:
            eventsWithAttribution =>
              val events = eventsWithAttribution.map(_.event)
              val attributions = eventsWithAttribution.map(_.attribution)
              events -> attributions
        _ <- replaySelectedEvents(testConversionAction, events, httpClient)
        _ <- ArtifactWriter[AttributionResult](attributionsSamplesPath).write(attributions)
      yield ()).assert

  withHttpClient(Stage.Verifier)
    .test("should check attributions".tag(delayed)): httpClient =>
      (for
        sampleAttributions <-
          ArtifactReader[AttributionResult](attributionsSamplesPath).readNonEmpty
            .map(_.toList)
        actualAttributions <-
          attributionsFor(testConversionAction, sampleAttributions, httpClient)
        actualCount =
          sampleAttributions.count: expected =>
            actualAttributions.exists: actual =>
              actual.conversionAction === testConversionAction &&
                expected.conversionAction === prodConversionAction &&
                actual.eventId === expected.eventId &&
                actual.attribution.channel === expected.attribution.channel
        result =
          val today = LocalDate.now()
          val expectedCount = sampleAttributions.length
          val minimumSuccessThreshold = 0.75d
          val successRate = if expectedCount > 0 then actualCount / expectedCount.toDouble else 0d
          val status =
            if successRate < minimumSuccessThreshold then VerificationResult.Status.Fail
            else VerificationResult.Status.Pass
          VerificationResult(
            date = today,
            expectedCount = expectedCount,
            actualCount = actualCount,
            minimumSuccessThreshold = minimumSuccessThreshold,
            successRate = successRate,
            status = status,
          )
        _ <-
          updateHistory(testConversionAction, result).flatMap: updated =>
            IO.println(s"⚠️ History file cannot be updated").whenA(!updated)
        _ <-
          IO.pure(result.actualCount)
            .assert(
              _ != 0d,
              "❌ CRITICAL FAILURE: Zero attributions found",
            )
        _ <-
          IO.pure(result.successRate)
            .assert(
              _ >= result.minimumSuccessThreshold,
              s"❌ THRESHOLD FAILURE: Success rate (${result.successRate}) is below ${result.minimumSuccessThreshold * 100}%",
            )
        _ <- IO
          .pure(result.actualCount < result.expectedCount)
          .ifM(
            IO.println(s"⚠️ PARTIAL SUCCESS: Found ${result.actualCount}/${result.expectedCount}"),
            IO.println(s"✅ PERFECT SUCCESS: All ${result.expectedCount} attributions verified"),
          )
      yield ()).assert

  private def updateHistory(
      conversionAction: ConversionAction,
      result: VerificationResult,
  ) =
    (for
      historyManager = HistoryManager(ArtifactStore[VerificationResult](verificationResultsPath))
      updatedHistory <- historyManager.updateHistory(result)
      pipelineHealthDashboard = PipelineHealthDashboard(healthDashboardPath)
      _ <- pipelineHealthDashboard.generate(conversionAction, updatedHistory)
    yield true).handleError(_ => false)

  private def withHttpClient(
      stage: Stage,
  ) =
    ResourceFunFixture:
      EmberClientBuilder
        .default[IO]
        .build
        .flatTap: httpClient =>
          Resource.eval:
            stage match
              case Stage.Uploader =>
                prepareTestData(
                  prodConversionAction,
                  httpClient,
                )
              case Stage.Verifier =>
                ArtifactReader[AttributionResult](attributionsSamplesPath).readAll.flatMap:
                  prepareTestData(
                    testConversionAction,
                    httpClient,
                    _,
                  )

object DriftCanarySuite:
  private val attributionsSamplesPath = "attributions_samples.jsonl"
  private val verificationResultsPath = "attributions_upload_history.jsonl"
  private val healthDashboardPath = "pipeline_dashboard.html"

  private val prodConversionAction = ConversionAction("purchase_prod")
  private val testConversionAction = ConversionAction("purchase_test")

  enum Stage:
    case Uploader, Verifier
