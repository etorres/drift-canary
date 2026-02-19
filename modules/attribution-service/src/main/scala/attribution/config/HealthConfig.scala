package es.eriktorr
package attribution.config

import attribution.refined.Refined.{asNonBlank, asUrlPathSegment, unsafeFrom}
import attribution.refined.RefinedError

import cats.derived.*
import cats.{Eq, Show}

final case class HealthConfig(
    livenessPath: HealthConfig.LivenessPath,
    readinessPath: HealthConfig.ReadinessPath,
    serviceName: HealthConfig.ServiceName,
) derives Show

object HealthConfig:
  opaque type LivenessPath <: String = String

  object LivenessPath:
    def fromString(
        value: String,
    ): Either[RefinedError, LivenessPath] =
      value.asUrlPathSegment("LivenessPath")

    def apply(value: String): LivenessPath =
      value.unsafeFrom(LivenessPath.fromString)

    given Eq[LivenessPath] = Eq.fromUniversalEquals
  end LivenessPath

  opaque type ReadinessPath <: String = String

  object ReadinessPath:
    def fromString(
        value: String,
    ): Either[RefinedError, ReadinessPath] =
      value.asUrlPathSegment("ReadinessPath")

    def apply(value: String): ReadinessPath =
      value.unsafeFrom(ReadinessPath.fromString)

    given Eq[ReadinessPath] = Eq.fromUniversalEquals
  end ReadinessPath

  opaque type ServiceName <: String = String

  object ServiceName:
    def fromString(
        value: String,
    ): Either[RefinedError, ServiceName] =
      value.asNonBlank("ServiceName")

    def apply(value: String): ServiceName =
      value.unsafeFrom(ServiceName.fromString)

    given Eq[ServiceName] = Eq.fromUniversalEquals
  end ServiceName

  val defaultLivenessPath: LivenessPath = LivenessPath("/healthz")
  val defaultReadinessPath: ReadinessPath = ReadinessPath("/ready")
