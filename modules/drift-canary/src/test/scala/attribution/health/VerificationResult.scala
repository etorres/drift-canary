package es.eriktorr
package attribution.health

import cats.derived.*
import cats.{Eq, Show}
import io.circe.Codec
import org.typelevel.cats.time.instances.localdate.given

import java.time.LocalDate

final case class VerificationResult(
    date: LocalDate,
    expectedCount: Int,
    actualCount: Double,
    minimumSuccessThreshold: Double,
    successRate: Double,
    status: VerificationResult.Status,
) derives Eq,
      Show,
      Codec

object VerificationResult:
  enum Status(val name: String) derives Eq, Show, Codec:
    case Fail extends Status("FAIL")
    case Pass extends Status("PASS")
