package es.eriktorr
package test.gen

import cats.collections.Range
import com.fortysevendeg.scalacheck.datetime.GenDateTime.genDateTimeWithinRange
import com.fortysevendeg.scalacheck.datetime.instances.jdk8.jdk8Instant
import com.fortysevendeg.scalacheck.datetime.jdk8.ArbitraryJdk8.{arbInstantJdk8, arbLocalDateJdk8}
import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.seconds as zonedDateTimeSeconds
import com.fortysevendeg.scalacheck.datetime.{Granularity, YearRange}
import org.scalacheck.Gen

import java.time.*
import java.time.temporal.ChronoUnit.SECONDS

object TemporalGenerators:
  private val yearRange = YearRange.between(1990, 2060)

  private val minLocalDate: LocalDate = LocalDate.of(yearRange.min, Month.JANUARY, 1)
  private val maxLocalDate: LocalDate = LocalDate.of(yearRange.max, Month.DECEMBER, 31)

  private val utc = ZoneOffset.UTC

  private val instantSeconds: Granularity[Instant] =
    new Granularity[Instant]:
      val normalize: Instant => Instant =
        (instant: Instant) => instant.truncatedTo(SECONDS)
      val description: String = "Seconds"

  val instantGen: Gen[Instant] =
    arbInstantJdk8(using
      granularity = zonedDateTimeSeconds,
      yearRange = yearRange,
    ).arbitrary

  val localDateGen: Gen[LocalDate] =
    arbLocalDateJdk8(using
      granularity = zonedDateTimeSeconds,
      yearRange = yearRange,
    ).arbitrary

  def outOfInstantRange(
      instantRange: Range[Instant],
  ): Gen[Instant] =
    Gen.frequency(
      1 -> beforeInstant(instantRange.start),
      1 -> afterInstant(instantRange.end),
    )

  private def beforeInstant(instant: Instant) =
    withinInstantRange(
      Range(
        minLocalDate.atTime(LocalTime.MIN).toInstant(utc),
        instant.minusSeconds(1),
      ),
    )

  private def afterInstant(instant: Instant) =
    withinInstantRange(
      Range(
        start = instant.plusSeconds(1),
        end = maxLocalDate.atTime(LocalTime.MAX).toInstant(utc),
      ),
    )

  def withinInstantRange(instantRange: Range[Instant]): Gen[Instant] =
    genDateTimeWithinRange(
      instantRange.start,
      Duration.ofSeconds(SECONDS.between(instantRange.start, instantRange.end)),
    )(using scDateTime = jdk8Instant, granularity = instantSeconds)
