package es.eriktorr
package test.infrastructure

import test.infrastructure.DeterministicUUIDGen.UUIDGenState

import cats.effect.std.UUIDGen
import cats.effect.{IO, Ref}

import java.util.UUID

final class DeterministicUUIDGen(
    stateRef: Ref[IO, UUIDGenState],
) extends UUIDGen[IO]:
  override def randomUUID: IO[UUID] =
    stateRef.flatModify: currentState =>
      val (headIO, next) = currentState.uuids match
        case ::(head, next) => (IO.pure(head), next)
        case Nil => (IO.raiseError(IllegalStateException("UUIDs exhausted")), List.empty)
      (currentState.copy(next), headIO)

object DeterministicUUIDGen:
  final case class UUIDGenState(uuids: List[UUID]):
    def one(newUuid: UUID): UUIDGenState =
      set(List(newUuid))

    def set(newUuids: List[UUID]): UUIDGenState =
      copy(uuids = newUuids)

  object UUIDGenState:
    def empty: UUIDGenState = UUIDGenState(List.empty)
