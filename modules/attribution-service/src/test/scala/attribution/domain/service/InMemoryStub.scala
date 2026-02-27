package es.eriktorr
package attribution.domain.service

import attribution.model.ConversionInstance
import attribution.model.ConversionInstance.{ConversionId, EventId}

import cats.effect.IO
import cats.effect.std.MapRef
import cats.implicits.*
import org.typelevel.log4cats.StructuredLogger

abstract class InMemoryStub[A <: ConversionInstance](
    mapRef: MapRef[IO, String, Option[A]],
)(using logger: StructuredLogger[IO]):
  final def addIfAbsent(value: A): IO[Unit] =
    mapRef(value.conversionInstancePath).flatModify: maybeDuplicated =>
      val (effect, update) =
        maybeDuplicated match
          case Some(duplicated) =>
            logger.info(
              show"Ignoring duplicated value: ${duplicated.conversionInstanceAsString}",
            ) -> duplicated
          case None =>
            IO.unit -> value
      (update.some, effect)

  final def findBy(
      conversionId: ConversionId,
  ): IO[Option[A]] =
    val conversionInstancePath =
      ConversionInstance.conversionInstancePath(
        conversionId,
      )
    mapRef(conversionInstancePath).get

  final def truncate: IO[Unit] = IO.unit
