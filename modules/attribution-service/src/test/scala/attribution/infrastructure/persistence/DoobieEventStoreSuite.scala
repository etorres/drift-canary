package es.eriktorr
package attribution.infrastructure.persistence

import attribution.gen.AttributionGenerators
import attribution.model.Event
import attribution.model.EventGenerators.eventGen
import attribution.support.DoobieStoreTestRunner

import cats.effect.IO
import cats.implicits.*
import org.scalacheck.effect.PropF.forAllF

final class DoobieEventStoreSuite extends DoobieStoreTestRunner with AttributionGenerators:
  test("should find an event by ID"):
    forAllF(findEventTestCaseGen):
      case (events, conversionId, expected) =>
        (for
          eventStore <- eventStoreFixture
          _ <- events.traverse_(eventStore.addIfAbsent)
          obtained <- eventStore.findBy(conversionId)
        yield obtained).assertEquals(expected)

  test("should filter events by conversion action and timestamp"):
    forAllF(filterEventsTestCaseGen()):
      case (events, (conversionAction, timestampRange, limit), expected) =>
        (for
          eventStore <- eventStoreFixture
          _ <- events.traverse_(eventStore.addIfAbsent)
          obtained <- eventStore.filterBy(conversionAction, timestampRange, limit)
        yield obtained.sortBy(_.eventId)).assertEquals(expected.sortBy(_.eventId))

  test("should ignore duplicated events"):
    forAllF(eventGen()): event =>
      (for
        eventStore <- eventStoreFixture
        _ <- eventStore.addIfAbsent(event)
        _ <- eventStore.addIfAbsent(event)
      yield ()).assert

  private def eventStoreFixture =
    IO(persistenceFixture()).map(_.eventStore)
