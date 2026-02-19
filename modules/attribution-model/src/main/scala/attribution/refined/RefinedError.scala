package es.eriktorr
package attribution.refined

import attribution.error.DomainError

import cats.Show
import cats.implicits.*

enum RefinedError(val message: String) extends DomainError(message):
  case EmptyOrBlankString(fieldName: String)
      extends RefinedError(show"$fieldName cannot be empty or blank")

  case NumberBetween[N: Show](fieldName: String, min: N, max: N)
      extends RefinedError(show"$fieldName should be a number between $min and $max")

  case InvalidUrlPathSegment(fieldName: String, sanitizedValue: String)
      extends RefinedError(
        show"$fieldName should be a valid URL path segment, but got: $sanitizedValue",
      )

object RefinedError:
  given Show[RefinedError] with
    override def show(
        refinedError: RefinedError,
    ): String = refinedError.message
