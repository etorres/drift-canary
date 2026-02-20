package es.eriktorr
package attribution.infrastructure.persistence

import attribution.gen.AttributionGenerators
import attribution.model.Event
import attribution.support.DoobieStoreTestRunner

import cats.effect.IO
import cats.implicits.*
import org.scalacheck.effect.PropF.forAllF

final class DoobieEventStoreSuite extends DoobieStoreTestRunner with AttributionGenerators:
  test("should find an event by ID"):
    forAllF(findEventTestCaseGen):
      case (events, conversionId, expected) =>
        (for
          eventSource <- eventSourceFixture
          _ <- events.traverse_(eventSource.addIfAbsent)
          obtained <- eventSource.findBy(conversionId)
        yield obtained).assertEquals(expected)

  test("should filter events by conversion action and timestamp"):
    forAllF(filterEventsTestCaseGen()):
      case (events, (conversionAction, timestampRange, limit), expected) =>
        (for
          eventSource <- eventSourceFixture
          _ <- events.traverse_(eventSource.addIfAbsent)
          obtained <- eventSource.filterBy(conversionAction, timestampRange, limit)
        yield obtained.sortBy(_.eventId)).assertEquals(expected.sortBy(_.eventId))

  private def eventSourceFixture =
    IO(persistenceFixture()).map(_.eventStore)
