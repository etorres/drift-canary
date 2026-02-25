package es.eriktorr
package test.gen

import org.scalacheck.Gen

import scala.util.chaining.*

object CollectionGenerators:
  def generateNDistinct[A](n: Int, genA: Gen[A]): Gen[List[A]] =
    Gen.containerOfN[Set, A](n, genA).map(_.toList)

  extension [A](self: List[A])
    @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
    def splitIntoTwoGroups: (List[A], List[A]) =
      splitIntoNGroups(self, 2).pipe: groups =>
        groups.head.toList -> groups.last.toList

    @SuppressWarnings(
      Array("org.wartremover.warts.IterableOps", "org.wartremover.warts.OptionPartial"),
    )
    def splitIntoThreeGroups: (List[A], List[A], List[A]) =
      splitIntoNGroups(self, 3).pipe: groups =>
        (
          groups.head.toList,
          groups.lift(1).get.toList,
          groups.last.toList,
        )

    def randomlySelectOne: Gen[(A, List[A])] =
      require(self.nonEmpty, "Cannot select from an empty list")
      for
        idx <- Gen.choose(0, self.size - 1)
        selected = self(idx)
        rest = self.take(idx) ++ self.drop(idx + 1)
      yield (selected, rest)

  def splitIntoNGroups[A](
      items: Seq[A],
      n: Int,
  ): Seq[Seq[A]] =
    require(n > 0, "Number of groups must be positive")
    require(items.size >= n, "Not enough items to split into the requested number of groups")
    val groups =
      items
        .grouped((items.length + n) / (n + 1))
        .toSeq
    (0 until n)
      .map: i =>
        groups.lift(i).getOrElse(Seq.empty)
