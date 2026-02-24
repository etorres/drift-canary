package es.eriktorr
package attribution.refined

import attribution.error.DomainError

import cats.Show
import cats.implicits.*

enum RefinedError(
    val message: String,
    val maybeCause: Option[Throwable] = Option.empty[Throwable],
) extends DomainError(message, maybeCause):
  case EmptyOrBlankString(fieldName: String)
      extends RefinedError(show"$fieldName cannot be empty or blank")

  case NumberBetween[N: Show](fieldName: String, min: N, max: N)
      extends RefinedError(show"$fieldName should be a number between $min and $max")

  case InvalidPostgresJdbcUrl(fieldName: String, sanitizedValue: String)
      extends RefinedError(
        show"$fieldName should be a valid Postgres JDBC URL, but got: $sanitizedValue",
      )

  case InvalidUrlPathSegment(fieldName: String, sanitizedValue: String)
      extends RefinedError(
        show"$fieldName should be a valid URL path segment, but got: $sanitizedValue",
      )

  case InvalidUUID(
      fieldName: String,
      sanitizedValue: String,
      cause: Throwable,
  ) extends RefinedError(
        show"$fieldName should be a valid UUID, but got: $sanitizedValue",
        cause.some,
      )

object RefinedError:
  given Show[RefinedError] with
    override def show(
        refinedError: RefinedError,
    ): String = refinedError.message
