package es.eriktorr
package attribution.infrastructure.persistence

import attribution.config.JdbcConfig
import attribution.error.DomainError

import cats.effect.{IO, Resource, ResourceIO}
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{MigrateErrorResult, MigrateResult}
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension

final class DatabaseMigrator(
    jdbcConfig: JdbcConfig,
):
  def migrate: IO[Unit] =
    IO.blocking:
      val configuration =
        Flyway
          .configure()
          .dataSource(
            jdbcConfig.connectUrl,
            jdbcConfig.username,
            jdbcConfig.password.value,
          )
          .failOnMissingLocations(true)
      configuration
        .getConfigurationExtension(classOf[PostgreSQLConfigurationExtension])
        .setTransactionalLock(false)
      val flyway = configuration.load()
      flyway.migrate()
    .flatMap:
        case errorResult: MigrateErrorResult =>
          IO.raiseError(DatabaseMigrator.Error.MigrationFailed(errorResult))
        case result: MigrateResult =>
          IO.raiseWhen(!result.success)(
            DatabaseMigrator.Error.MigrationFailed(
              MigrateErrorResult(
                result,
                RuntimeException("Unsuccessful result"),
              ),
            ),
          )

object DatabaseMigrator:
  def make(
      jdbcConfig: JdbcConfig,
  ): ResourceIO[DatabaseMigrator] =
    Resource.pure(DatabaseMigrator(jdbcConfig))

  enum Error(val message: String) extends DomainError(message):
    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    case MigrationFailed(errorResult: MigrateErrorResult)
        extends Error(
          s"code: ${errorResult.error.errorCode}, message: ${errorResult.error.message}, cause: ${errorResult.error.cause}",
        )
