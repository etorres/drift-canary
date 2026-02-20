package es.eriktorr
package attribution.domain.service

import attribution.model.ConversionInstance.ConversionAction
import attribution.model.Event

import cats.collections.Range
import cats.effect.IO
import cats.effect.std.MapRef
import org.typelevel.cats.time.instances.instant.given
import org.typelevel.log4cats.StructuredLogger

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap as JConcurrentHashMap

final class EventStoreStub(
    concurrentHashMap: JConcurrentHashMap[String, Event],
    mapRef: MapRef[IO, String, Option[Event]],
)(using logger: StructuredLogger[IO])
    extends InMemoryStub[Event](mapRef)
    with EventStore:
  override def filterBy(
      conversionAction: ConversionAction,
      timestampRange: Range[Instant],
      maxResults: Int,
  ): IO[List[Event]] =
    allEvents.map:
      _.filter: event =>
        event.conversionAction.eq(conversionAction)
          && timestampRange.contains(event.timestamp)
      .take(maxResults)

  private def allEvents =
    import scala.jdk.CollectionConverters.*
    IO.delay(concurrentHashMap.values()).map(_.asScala.toList)

object EventStoreStub:
  def inMemory(using logger: StructuredLogger[IO]): IO[EventStore] =
    IO.delay:
      val concurrentHashMap = JConcurrentHashMap[String, Event]()
      val mapRef = MapRef.fromConcurrentHashMap[IO, String, Event](concurrentHashMap)
      EventStoreStub(concurrentHashMap, mapRef)
