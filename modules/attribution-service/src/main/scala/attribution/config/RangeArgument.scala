package es.eriktorr
package attribution.config

import cats.collections.Range
import cats.data.{Validated, ValidatedNel}
import cats.implicits.*
import com.monovore.decline.Argument

trait RangeArgument:
  given intRangeArgument: Argument[Range[Int]] = new Argument[Range[Int]]:
    override def read(string: String): ValidatedNel[String, Range[Int]] =
      import scala.language.unsafeNulls
      string.split(":", 2) match
        case Array(minimum, maximum) => (intFrom(minimum), intFrom(maximum)).mapN(Range.apply)
        case _ => show"Invalid minimum:maximum range: $string".invalidNel
    override def defaultMetavar: String = "minimum:maximum"

  private def intFrom(string: String) = string.toIntOption match
    case Some(value) => value.validNel
    case None => show"Invalid number format: $string".invalidNel

object RangeArgument extends RangeArgument
