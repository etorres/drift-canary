package es.eriktorr
package attribution.infrastructure.persistence

import attribution.domain.service.{AttributionStore, EventStore}

import cats.effect.{IO, ResourceIO}
import cats.implicits.*
import doobie.h2.H2Transactor
import doobie.implicits.*
import doobie.{ExecutionContexts, Transactor}

import java.nio.file.Path

object DatabaseModule:
  def make(
      dbPath: Path,
      poolSize: Int = 2,
  ): ResourceIO[Persistence] =
    ExecutionContexts
      .fixedThreadPool[IO](poolSize)
      .flatMap: executionContext =>
        H2Transactor
          .newH2Transactor[IO](
            url = show"jdbc:h2:file:${dbPath.toString};DB_CLOSE_DELAY=-1",
            user = "sa",
            pass = "",
            connectEC = executionContext,
          )
          .evalTap(initializeSchema)
          .map: transactor =>
            (
              eventStore = DoobieEventStore(transactor),
              attributionStore = DoobieAttributionStore(transactor),
            )

  private def initializeSchema(
      transactor: Transactor[IO],
  ) =
    (
      DoobieEventStore.createEventsSchema,
      DoobieAttributionStore.createAttributionsSchema,
    ).mapN(_ + _).transact(transactor).void

  type Persistence = (
      eventStore: EventStore,
      attributionStore: AttributionStore,
  )
