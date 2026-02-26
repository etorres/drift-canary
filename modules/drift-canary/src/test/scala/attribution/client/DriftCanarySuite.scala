package es.eriktorr
package attribution.client

import attribution.client.DriftCanarySuite.*
import attribution.infrastructure.ArtifactWriter
import attribution.model.ConversionInstance.ConversionAction
import attribution.model.Event
import attribution.support.DriftCanaryTestRunner
import test.utils.TestFilters.{delayed, scheduled}

import cats.effect.{IO, Resource}
import cats.implicits.*
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.ember.client.EmberClientBuilder

@SuppressWarnings(Array("org.wartremover.warts.Any"))
final class DriftCanarySuite extends DriftCanaryTestRunner:
  withHttpClient()
    .test("should pick and upload selected events".tag(scheduled).only): // TODO: remove only
      httpClient =>
        (for
          events <- selectGoldenInputEvents(httpClient, prodConversionAction)
          _ <- replaySelectedEvents(httpClient, testConversionAction, events)
          _ <- ArtifactWriter[Event](conversionSamplesPath).write(events)
        yield ()).assert

  withHttpClient()
    .test("should check attributions".tag(delayed)): httpClient =>
      httpClient
        .expect[String]("http://localhost:8080/hello/Ember")
        .assertEquals("123")

  private def withHttpClient() =
    ResourceFunFixture:
      EmberClientBuilder
        .default[IO]
        .build
        .flatTap: httpClient =>
          Resource.eval:
            prepareTestData(
              prodConversionAction,
              httpClient,
            )

object DriftCanarySuite:
  private val conversionSamplesPath = "conversion_samples.jsonl"

  private val prodConversionAction = ConversionAction("purchase_prod")
  private val testConversionAction = ConversionAction("purchase_test")

/* TODO
Event selector & uploader
- Select from the "purchase_prod" up to 10 events uploaded two days ago, along with their attributions.
- Upload the events to the "purchase_test" conversion action.

Attributions verifier
- Get the attribution for the 10 events.
- Compare the attributions to the "purchase_prod".
 */
