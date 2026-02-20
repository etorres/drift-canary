package es.eriktorr
package attribution.support

import attribution.infrastructure.persistence.DatabaseModule

import cats.effect.IO
import fs2.io.file.Files
import munit.catseffect.IOFixture
import munit.AnyFixture
import org.scalacheck.Test

abstract class DoobieStoreTestRunner extends AttributionTestRunner:
  protected lazy val persistenceFixture: IOFixture[DatabaseModule.Persistence] =
    ResourceSuiteLocalFixture(
      "doobie-store-test",
      Files[IO]
        .tempFile(None, "drift-test-", ".db", None)
        .flatMap: path =>
          DatabaseModule.make(path.toNioPath),
    )

  final override def munitFixtures: Seq[AnyFixture[?]] =
    List(persistenceFixture)

  final override def scalaCheckTestParameters: Test.Parameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(1)
