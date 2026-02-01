package es.eriktorr
package common.types

object Refined extends StringRefinements:
  extension [A, B](self: A)
    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def unsafeFrom(
        builder: A => Either[RefinedError, B],
    ): B =
      builder(self) match
        case Left(error) => throw error
        case Right(valid) => valid
