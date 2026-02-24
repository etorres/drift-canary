package es.eriktorr
package attribution.infrastructure.persistence

import attribution.domain.service.EventStore
import attribution.infrastructure.persistence.DatabaseExtensions.ignoreDuplicates
import attribution.model.ConversionInstance.{ConversionAction, ConversionId}
import attribution.model.Event

import cats.collections.Range
import cats.effect.IO
import doobie.Transactor
import doobie.implicits.*
import doobie.postgres.implicits.given

import java.time.Instant

final class DoobieEventStore(
    transactor: Transactor[IO],
) extends EventStore
    with DoobieEventProtocol:
  override def addIfAbsent(
      event: Event,
  ): IO[Unit] =
    sql"""|
          |INSERT INTO events (conversion_action, event_id, user_id, timestamp, source, amount)
          |VALUES (
          |  ${event.conversionAction},
          |  ${event.eventId},
          |  ${event.userId},
          |  ${event.timestamp},
          |  ${event.source},
          |  ${event.amount}
          |)
          |""".stripMargin.update.run
      .ignoreDuplicates(transactor)

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
