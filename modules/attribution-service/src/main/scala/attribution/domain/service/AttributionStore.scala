package es.eriktorr
package attribution.domain.service

import attribution.model.Attribution
import attribution.model.ConversionInstance.ConversionId

import cats.effect.IO

trait AttributionStore:
  def addIfAbsent(
      attribution: Attribution,
  ): IO[Unit]

  def findBy(
      conversionId: ConversionId,
  ): IO[Option[Attribution]]
