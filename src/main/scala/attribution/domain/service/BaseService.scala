package es.eriktorr
package attribution.domain.service

import attribution.domain.model.ConversionInstance
import attribution.domain.model.ConversionInstance.EventId

import cats.effect.IO
import cats.effect.std.MapRef
import cats.implicits.showInterpolator
import cats.syntax.all.catsSyntaxOptionId
import org.typelevel.log4cats.StructuredLogger

abstract class BaseService[A <: ConversionInstance](
    mapRef: MapRef[IO, String, Option[A]],
)(using logger: StructuredLogger[IO]):
  def addIfAbsent(value: A): IO[Unit] =
    mapRef(value.conversionInstancePath).flatModify: maybeDuplicated =>
      val (effect, update) =
        maybeDuplicated match
          case Some(duplicated) =>
            logger.info(
              s"Ignoring duplicated value: ${duplicated.conversionInstanceAsString}",
            ) -> duplicated
          case None =>
            IO.unit -> value
      (update.some, effect)

  def find(
      conversionAction: ConversionInstance.ConversionAction,
      eventId: EventId,
  ): IO[Option[A]] =
    val conversionInstancePath =
      ConversionInstance.conversionInstancePath(
        conversionAction,
        eventId,
      )
    mapRef(conversionInstancePath).get
