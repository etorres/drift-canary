package es.eriktorr
package attribution.refined

import attribution.refined.numeric.NumericRefinements
import attribution.refined.string.StringRefinements

object Refined extends NumericRefinements with StringRefinements:
  extension [A, B](self: A)
    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def unsafeFrom(
        builder: A => Either[RefinedError, B],
    ): B =
      builder(self) match
        case Left(error) => throw error
        case Right(valid) => valid
