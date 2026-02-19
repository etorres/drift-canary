package es.eriktorr
package attribution.config

import attribution.config.HttpServerConfig.MaxActiveRequests
import attribution.refined.Refined.{asNumberBetween, unsafeFrom}
import attribution.refined.RefinedError

import cats.collections.Range
import cats.derived.*
import cats.{Eq, Show}
import com.comcast.ip4s.{host, port, Host, Port}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

final case class HttpServerConfig(
    host: Host,
    maxActiveRequests: MaxActiveRequests,
    port: Port,
    timeout: FiniteDuration,
) derives Show

object HttpServerConfig:
  opaque type MaxActiveRequests <: Long = Long

  object MaxActiveRequests:
    def fromNumber(
        value: Long,
    ): Either[RefinedError, MaxActiveRequests] =
      value.asNumberBetween("MaxActiveRequests", Range(1L, 4096L))

    def apply(value: Long): MaxActiveRequests =
      value.unsafeFrom(MaxActiveRequests.fromNumber)

    given Eq[MaxActiveRequests] = Eq.fromUniversalEquals
  end MaxActiveRequests

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val defaultHost: Host = host"localhost"
  val defaultMaxActiveRequests: MaxActiveRequests =
    MaxActiveRequests(128L)
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val defaultPort: Port = port"8080"
  val defaultTimeout: FiniteDuration = 40.seconds
