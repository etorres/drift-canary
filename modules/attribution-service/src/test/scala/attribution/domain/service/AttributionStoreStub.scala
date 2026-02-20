package es.eriktorr
package attribution.domain.service

import attribution.model.Attribution

import cats.effect.IO
import cats.effect.std.MapRef
import org.typelevel.log4cats.StructuredLogger

final class AttributionStoreStub(
    mapRef: MapRef[IO, String, Option[Attribution]],
)(using logger: StructuredLogger[IO])
    extends InMemoryStub[Attribution](mapRef)
    with AttributionStore

object AttributionStoreStub:
  def inMemory(using logger: StructuredLogger[IO]): IO[AttributionStore] =
    MapRef[IO, String, Attribution].map(AttributionStoreStub.apply)
