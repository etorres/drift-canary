package es.eriktorr
package attribution.infrastructure.persistence

import attribution.gen.AttributionGenerators
import attribution.model.AttributionGenerators.attributionGen
import attribution.support.DoobieStoreTestRunner

import cats.effect.IO
import cats.implicits.*
import org.scalacheck.effect.PropF.forAllF

final class DoobieAttributionStoreSuite extends DoobieStoreTestRunner with AttributionGenerators:
  test("should find an attribution by ID"):
    forAllF(findAttributionTestCaseGen):
      case ((events, attributions), conversionId, expected) =>
        (for
          (eventStore, attributionStore) <- IO(persistenceFixture())
          _ <- events.traverse_(eventStore.addIfAbsent)
          _ <- attributions.traverse_(attributionStore.addIfAbsent)
          obtained <- attributionStore.findBy(conversionId)
        yield obtained).assertEquals(expected)

  test("should ignore duplicated attributions"):
    forAllF(attributionGen()): attribution =>
      (for
        attributionStore <- attributionStoreFixture
        _ <- attributionStore.addIfAbsent(attribution)
        _ <- attributionStore.addIfAbsent(attribution)
      yield ()).assert

  test("should truncate all data"):
    forAllF(findAttributionTestCaseGen):
      case ((events, attributions), conversionId, expected) =>
        (for
          (eventStore, attributionStore) <- IO(persistenceFixture())
          _ <- events.traverse_(eventStore.addIfAbsent)
          _ <- attributions.traverse_(attributionStore.addIfAbsent)
          _ <- eventStore.truncate
          obtained <- attributionStore.findBy(conversionId)
        yield obtained).assert(_.isEmpty)

  private def attributionStoreFixture =
    IO(persistenceFixture()).map(_.attributionStore)
