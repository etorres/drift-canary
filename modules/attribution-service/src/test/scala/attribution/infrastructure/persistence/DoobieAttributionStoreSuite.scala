package es.eriktorr
package attribution.infrastructure.persistence

import attribution.gen.AttributionGenerators
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
