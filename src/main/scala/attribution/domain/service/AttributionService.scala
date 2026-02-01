package es.eriktorr
package attribution.domain.service

import attribution.domain.model.Attribution
import attribution.domain.model.ConversionInstance.{ConversionAction, EventId}

import cats.effect.IO
import cats.effect.std.MapRef
import org.typelevel.log4cats.StructuredLogger

trait AttributionService:
  def addIfAbsent(attribution: Attribution): IO[Unit]
  def find(
      conversionAction: ConversionAction,
      eventId: EventId,
  ): IO[Option[Attribution]]

object AttributionService:
  def inMemory(using logger: StructuredLogger[IO]): IO[AttributionService] =
    MapRef[IO, String, Attribution].map(InMemory.apply)

  final class InMemory(
      mapRef: MapRef[IO, String, Option[Attribution]],
  )(using logger: StructuredLogger[IO])
      extends BaseService[Attribution](mapRef)
      with AttributionService
