package es.eriktorr
package attribution.domain.service

import attribution.model.ConversionInstance.{ConversionAction, ConversionId}
import attribution.model.Event

import cats.collections.Range
import cats.effect.IO

import java.time.Instant

final class EventService(
    store: EventStore,
):
  def record(event: Event): IO[Unit] =
    store.addIfAbsent(event)

  def get(
      conversionId: ConversionId,
  ): IO[Option[Event]] =
    store.findBy(conversionId)

  def query(
      conversionAction: ConversionAction,
      timestampRange: Range[Instant],
      maxResults: Int,
  ): IO[List[Event]] =
    store.filterBy(
      conversionAction,
      timestampRange,
      maxResults,
    )

  def truncate: IO[Unit] =
    store.truncate
