package es.eriktorr
package attribution.infrastructure.persistence

import attribution.domain.service.AttributionStore
import attribution.model.Attribution
import attribution.model.ConversionInstance.ConversionId

import cats.effect.IO
import cats.implicits.*
import doobie.{ConnectionIO, Transactor}
import doobie.implicits.*

final class DoobieAttributionStore(
    transactor: Transactor[IO],
) extends AttributionStore
    with DoobieAttributionProtocol:
  override def addIfAbsent(
      attribution: Attribution,
  ): IO[Unit] =
    sql"""|
          |MERGE INTO attributions (conversion_action, event_id, channel, model_version)
          |KEY (conversion_action, event_id)
          |VALUES (
          |  ${attribution.conversionAction},
          |  ${attribution.eventId},
          |  ${attribution.channel},
          |  ${attribution.modelVersion}
          |)
          |""".stripMargin.update.run
      .transact(transactor)
      .ensure(RuntimeException("Insert failed: affected rows != 1"))(_ == 1)
      .void

  override def findBy(
      conversionId: ConversionId,
  ): IO[Option[Attribution]] =
    val (conversionAction, eventId) = conversionId
    sql"""|
          |SELECT
          |  conversion_action, event_id, channel, model_version
          |FROM attributions
          |WHERE conversion_action = $conversionAction
          |  AND event_id = $eventId
          |""".stripMargin
      .query[Attribution]
      .option
      .transact(transactor)

object DoobieAttributionStore:
  val createAttributionsSchema: ConnectionIO[Int] =
    sql"""|
          |CREATE TABLE IF NOT EXISTS attributions (
          |  conversion_action VARCHAR(32) NOT NULL,
          |  event_id UUID NOT NULL,
          |  channel ENUM('Organic','PaidSearch','PaidSocial'),
          |  model_version ENUM('v1','v2'),
          |  PRIMARY KEY (conversion_action, event_id),
          |  FOREIGN KEY (conversion_action, event_id)
          |    REFERENCES events(conversion_action, event_id)
          |    ON DELETE CASCADE
          |)
          |""".stripMargin.update.run
