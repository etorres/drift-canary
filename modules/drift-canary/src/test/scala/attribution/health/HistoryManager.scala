package es.eriktorr
package attribution.health

import attribution.infrastructure.ArtifactStore.ArtifactStore

import cats.effect.IO

final class HistoryManager(
    artifactStore: ArtifactStore[VerificationResult],
):
  def updateHistory(
      newResult: VerificationResult,
  ): IO[List[VerificationResult]] =
    for
      existingHistory <- artifactStore.readAll
      maxCountByDate = (newResult :: existingHistory)
        .groupMapReduce(_.date)(identity) { (x, y) =>
          if x.actualCount > y.actualCount then x else y
        }
        .values
        .toList
      updatedHistory = maxCountByDate.sortBy(_.date).reverse.takeRight(30)
      _ <- artifactStore.write(updatedHistory)
    yield updatedHistory
