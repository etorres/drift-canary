package es.eriktorr
package attribution.infrastructure.persistence

import cats.effect.IO
import cats.implicits.*
import doobie.implicits.*
import doobie.{ConnectionIO, Transactor}

import java.sql.SQLException
import java.util.regex.Pattern

object DatabaseExtensions:
  extension [R](self: ConnectionIO[R])
    def ignoreDuplicates(
        transactor: Transactor[IO],
    ): IO[Unit] =
      self
        .transact(transactor)
        .void
        .handleErrorWith:
          case e: SQLException if isUniqueViolation(e) || isForeignKeyViolation(e) =>
            IO.unit
          case other => IO.raiseError(other)

    private def isUniqueViolation(
        exception: SQLException,
    ) =
      isPostgresError(exception, PostgresSqlState.UniqueViolation)
        && matchErrorMessage(
          exception.getMessage,
          "ERROR: duplicate key value violates unique constraint",
        )

    private def isForeignKeyViolation(
        exception: SQLException,
    ) =
      isPostgresError(exception, PostgresSqlState.ForeignKeyViolation)
        && matchErrorMessage(
          exception.getMessage,
          "ERROR: insert or update on table \".*\" violates foreign key constraint",
        )

    private def isPostgresError(
        exception: SQLException,
        sqlState: PostgresSqlState,
    ) =
      (
        Option(exception.getErrorCode),
        Option(exception.getSQLState).flatMap(PostgresSqlState.fromString),
      ).tupled.contains(0, sqlState)

    private def matchErrorMessage(
        maybeMessage: String | Null,
        regex: String,
    ) =
      val message = Option(maybeMessage).getOrElse("")
      val pattern = Pattern.compile(regex, Pattern.DOTALL)
      pattern.matcher(message).find()
