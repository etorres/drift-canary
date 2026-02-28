package es.eriktorr
package attribution.domain.service

import attribution.model.ConversionInstance.{ConversionAction, ConversionId}
import attribution.model.Event

import cats.collections.Range
import cats.effect.IO

import java.time.Instant

trait EventStore:
  def addIfAbsent(
      event: Event,
  ): IO[Unit]

  def findBy(
      conversionId: ConversionId,
  ): IO[Option[Event]]

  def filterBy(
      conversionAction: ConversionAction,
      timestampRange: Range[Instant],
      maxResults: Int,
  ): IO[List[Event]]

  def truncate: IO[Unit]
