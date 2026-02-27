package es.eriktorr
package attribution.refined.numeric

import attribution.refined.RefinedError

import cats.collections.Range
import cats.implicits.*

trait NumericRefinements:
  extension [A <: Long](self: Long)
    @SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
    def asNumberBetween(
        fieldName: String,
        range: Range[Long],
    ): Either[RefinedError, Long] =
      if range.contains(self) then self.asRight
      else RefinedError.NumberBetween(fieldName, range.start, range.end).asLeft

  extension [A <: Double](self: Double)
    @SuppressWarnings(Array("org.wartremover.contrib.warts.UnsafeInheritance"))
    def asNumberBetween(
        fieldName: String,
        range: Range[Double],
    ): Either[RefinedError, Double] =
      if range.contains(self) then self.asRight
      else RefinedError.NumberBetween(fieldName, range.start, range.end).asLeft
