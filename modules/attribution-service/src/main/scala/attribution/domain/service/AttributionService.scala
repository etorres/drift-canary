package es.eriktorr
package attribution.domain.service

import attribution.model.Attribution
import attribution.model.ConversionInstance.ConversionId

import cats.effect.IO

final class AttributionService(
    store: AttributionStore,
):
  def record(attribution: Attribution): IO[Unit] =
    store.addIfAbsent(attribution)

  def get(
      conversionId: ConversionId,
  ): IO[Option[Attribution]] =
    store.findBy(conversionId)

  def truncate: IO[Unit] =
    store.truncate
