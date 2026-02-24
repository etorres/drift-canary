package es.eriktorr
package attribution.infrastructure.persistence

enum PostgresSqlState(val value: Int):
  case ForeignKeyViolation extends PostgresSqlState(23503)
  case UniqueViolation extends PostgresSqlState(23505)

object PostgresSqlState:
  def fromString(
      string: String,
  ): Option[PostgresSqlState] =
    string.toIntOption.flatMap: value =>
      PostgresSqlState.values.find(_.value == value)
