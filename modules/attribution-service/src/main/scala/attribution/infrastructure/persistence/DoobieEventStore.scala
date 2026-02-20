package es.eriktorr
package attribution.infrastructure.persistence

import attribution.domain.service.EventStore
import attribution.model.ConversionInstance.{ConversionAction, ConversionId}
import attribution.model.Event

import cats.collections.Range
import cats.effect.IO
import cats.implicits.*
import doobie.{ConnectionIO, Transactor}
import doobie.implicits.*
import doobie.implicits.javatimedrivernative.*

import java.time.Instant

final class DoobieEventStore(
    transactor: Transactor[IO],
) extends EventStore
    with DoobieEventProtocol:
  override def addIfAbsent(
      event: Event,
  ): IO[Unit] =
    sql"""|
          |MERGE INTO events (conversion_action, event_id, user_id, timestamp, source, amount)
          |KEY (conversion_action, event_id)
          |VALUES (
          |  ${event.conversionAction},
          |  ${event.eventId},
          |  ${event.userId},
          |  ${event.timestamp},
          |  ${event.source},
          |  ${event.amount}
          |)
          |""".stripMargin.update.run
      .transact(transactor)
      .ensure(RuntimeException("Insert failed: affected rows != 1"))(_ == 1)
      .void

  override def findBy(
      conversionId: ConversionId,
  ): IO[Option[Event]] =
    val (conversionAction, eventId) = conversionId
    sql"""|
          |SELECT
          |  conversion_action, event_id, user_id, timestamp, source, amount
          |FROM events
          |WHERE conversion_action = $conversionAction
          |  AND event_id = $eventId
          |""".stripMargin
      .query[Event]
      .option
      .transact(transactor)

  override def filterBy(
      conversionAction: ConversionAction,
      timestampRange: Range[Instant],
      maxResults: Int,
  ): IO[List[Event]] =
    sql"""|
          |SELECT
          |  conversion_action, event_id, user_id, timestamp, source, amount
          |FROM events
          |WHERE conversion_action = $conversionAction
          |  AND timestamp BETWEEN ${timestampRange.start} AND ${timestampRange.end}
          |LIMIT $maxResults
          |""".stripMargin
      .query[Event]
      .to[List]
      .transact(transactor)

object DoobieEventStore:
  val createEventsSchema: ConnectionIO[Int] =
    (createEventsTable, createEventsIndex).mapN(_ + _)

  private lazy val createEventsTable =
    sql"""|
          |CREATE TABLE IF NOT EXISTS events (
          |  conversion_action VARCHAR(32) NOT NULL,
          |  event_id UUID NOT NULL,
          |  user_id VARCHAR(32) NOT NULL,
          |  timestamp TIMESTAMP NOT NULL,
          |  source ENUM('Facebook','Google','Other'),
          |  amount DECIMAL(19, 4) NOT NULL,
          |  PRIMARY KEY (conversion_action, event_id)
          |)
          |""".stripMargin.update.run

  private lazy val createEventsIndex =
    sql"""|
          |CREATE INDEX idx_events_timestamp ON events(timestamp)
          |""".stripMargin.update.run
