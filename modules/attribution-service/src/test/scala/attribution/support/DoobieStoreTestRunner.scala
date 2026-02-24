package es.eriktorr
package attribution.support

import attribution.config.JdbcTestConfig
import attribution.infrastructure.persistence.DatabaseModule

import munit.AnyFixture
import munit.catseffect.IOFixture
import org.scalacheck.Test

abstract class DoobieStoreTestRunner extends AttributionTestRunner:
  protected lazy val persistenceFixture: IOFixture[DatabaseModule.Persistence] =
    ResourceSuiteLocalFixture(
      "doobie-store-test",
      DatabaseModule.make(JdbcTestConfig.LocalContainer.config),
    )

  final override def munitFixtures: Seq[AnyFixture[?]] =
    List(persistenceFixture)

  final override def scalaCheckTestParameters: Test.Parameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(1)
