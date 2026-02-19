package es.eriktorr
package attribution.api

import attribution.api.HealthServiceStub.HealthServiceState
import attribution.config.HealthConfig
import attribution.config.HealthConfig.{LivenessPath, ReadinessPath, ServiceName}

import cats.effect.{IO, Ref}

final class HealthServiceStub(
    stateRef: Ref[IO, HealthServiceState],
) extends HealthService:
  override def isReady: IO[Boolean] =
    stateRef.get.map(_.ready)

  override def livenessPath: LivenessPath =
    HealthConfig.defaultLivenessPath

  override def markReady: IO[Unit] =
    stateRef.update(_.copy(true))

  override def markUnready: IO[Unit] =
    stateRef.update(_.copy(false))

  override def readinessPath: ReadinessPath =
    HealthConfig.defaultReadinessPath

  override def serviceName: ServiceName =
    ServiceName("ServiceName")

object HealthServiceStub:
  final case class HealthServiceState(ready: Boolean)

  object HealthServiceState:
    val unready: HealthServiceState = HealthServiceState(false)
