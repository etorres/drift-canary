package es.eriktorr
package attribution.api

import attribution.config.HealthConfig
import attribution.config.HealthConfig.{LivenessPath, ReadinessPath, ServiceName}

import cats.effect.{IO, Ref, Resource}
import org.typelevel.log4cats.StructuredLogger

trait HealthService:
  def isReady: IO[Boolean]
  def livenessPath: LivenessPath
  def markReady: IO[Unit]
  def markUnready: IO[Unit]
  def readinessPath: ReadinessPath
  def serviceName: ServiceName

object HealthService:
  def resourceWith(
      healthConfig: HealthConfig,
      ready: Boolean = false,
  )(using logger: StructuredLogger[IO]): Resource[IO, HealthService] =
    Resource.make(
      for
        readyRef <- Ref.of[IO, Boolean](ready)
        healthService = new HealthService():
          override def isReady: IO[Boolean] = readyRef.get

          override def livenessPath: LivenessPath =
            healthConfig.livenessPath

          override def markReady: IO[Unit] =
            for
              _ <- logger.info("HealthService marked as ready")
              _ <- readyRef.set(true)
            yield ()

          override def markUnready: IO[Unit] =
            for
              _ <- logger.info("HealthService marked as unready")
              _ <- readyRef.set(false)
            yield ()

          override def readinessPath: ReadinessPath =
            healthConfig.readinessPath

          override def serviceName: ServiceName =
            healthConfig.serviceName
      yield healthService,
    )(_.markUnready)
