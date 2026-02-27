package es.eriktorr
package attribution.infrastructure.persistence

import attribution.domain.service.AttributionStore
import attribution.infrastructure.persistence.DatabaseExtensions.ignoreDuplicates
import attribution.model.Attribution
import attribution.model.ConversionInstance.ConversionId

import cats.effect.IO
import doobie.Transactor
import doobie.implicits.*

final class DoobieAttributionStore(
    transactor: Transactor[IO],
) extends AttributionStore
    with DoobieAttributionProtocol:
  override def addIfAbsent(
      attribution: Attribution,
  ): IO[Unit] =
    sql"""|
          |INSERT INTO attributions (conversion_action, event_id, channel, version)
          |VALUES (
          |  ${attribution.conversionAction},
          |  ${attribution.eventId},
          |  ${attribution.channel},
          |  ${attribution.modelVersion}
          |)
          |""".stripMargin.update.run
      .ignoreDuplicates(transactor)

  override def findBy(
      conversionId: ConversionId,
  ): IO[Option[Attribution]] =
    val (conversionAction, eventId) = conversionId
    sql"""|
          |SELECT
          |  conversion_action, event_id, channel, version
          |FROM attributions
          |WHERE conversion_action = $conversionAction
          |  AND event_id = $eventId
          |""".stripMargin
      .query[Attribution]
      .option
      .transact(transactor)

  override def truncate: IO[Unit] =
    sql"TRUNCATE TABLE attributions CASCADE".update.run
      .transact(transactor)
      .void
