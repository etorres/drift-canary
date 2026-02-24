package es.eriktorr
package attribution.infrastructure.persistence

import attribution.gen.AttributionGenerators
import attribution.support.DoobieStoreTestRunner

import cats.effect.IO
import cats.implicits.*
import es.eriktorr.attribution.model.AttributionGenerators.attributionGen
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

  private def attributionStoreFixture =
    IO(persistenceFixture()).map(_.attributionStore)
