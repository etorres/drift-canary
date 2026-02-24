package es.eriktorr
package attribution.config

import attribution.config.JdbcConfig.{ConnectUrl, Password, Username}
import attribution.security.Secret

import cats.collections.Range

enum JdbcTestConfig(
    val config: JdbcConfig,
    val database: String,
):
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  case LocalContainer
      extends JdbcTestConfig(
        JdbcConfig(
          Range(JdbcTestConfig.minConnections, JdbcTestConfig.maxConnections),
          ConnectUrl(
            s"jdbc:postgresql://${JdbcTestConfig.postgresHost}/${JdbcTestConfig.attributionDatabase}",
          ),
          Secret(Password(JdbcTestConfig.postgresPassword)),
          Username(JdbcTestConfig.postgresUsername),
        ),
        JdbcTestConfig.attributionDatabase,
      )

object JdbcTestConfig:
  final private val maxConnections = 3

  final private val minConnections = 1

  final private val postgresHost = "postgres.local:5432"

  final private val postgresPassword = "changeMe"

  final private val postgresUsername = "test"

  final private val attributionDatabase = "attribution"
