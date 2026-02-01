package es.eriktorr
package attribution.domain.service

import attribution.domain.model.Attribution.ModelVersion
import attribution.domain.model.{Attribution, Event}
import attribution.domain.service.AttributionLogic

import cats.effect.{IO, Temporal}

import scala.concurrent.duration.DurationInt

final class EventProcessor(
    attributionService: AttributionService,
    val modelVersion: ModelVersion,
):
  def process(event: Event): IO[Unit] =
    (Temporal[IO].sleep(10.seconds) >>
      (for
        channel = AttributionLogic.attribute(event, modelVersion)
        attribution = Attribution(
          eventId = event.eventId,
          conversionAction = event.conversionAction,
          channel = channel,
          modelVersion = modelVersion,
        )
        _ <- attributionService.addIfAbsent(attribution)
      yield ())).start.void
