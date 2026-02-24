package es.eriktorr
package attribution.infrastructure.persistence

import attribution.config.JdbcConfig
import attribution.domain.service.{AttributionStore, EventStore}

import cats.effect.{IO, ResourceIO}
import cats.implicits.*
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor

import scala.concurrent.duration.DurationInt

object DatabaseModule:
  def make(
      jdbcConfig: JdbcConfig,
  ): ResourceIO[Persistence] =
    DatabaseMigrator
      .make(jdbcConfig)
      .evalMap(_.migrate)
      .as:
        val config = new HikariConfig()
        config.setJdbcUrl(jdbcConfig.connectUrl)
        config.setUsername(jdbcConfig.username)
        config.setPassword(jdbcConfig.password.value)
        config.setMinimumIdle(jdbcConfig.connections.start)
        config.setMaximumPoolSize(jdbcConfig.connections.end)
        config.setLeakDetectionThreshold(2000L)
        config.setConnectionTimeout(30.seconds.toMillis)
        config
      .flatMap(HikariTransactor.fromHikariConfig[IO](_))
      .map: transactor =>
        (
          eventStore = DoobieEventStore(transactor),
          attributionStore = DoobieAttributionStore(transactor),
        )

  type Persistence = (
      eventStore: EventStore,
      attributionStore: AttributionStore,
  )
