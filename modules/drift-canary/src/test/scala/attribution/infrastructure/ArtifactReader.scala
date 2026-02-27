package es.eriktorr
package attribution.infrastructure

import attribution.infrastructure.ArtifactReader.{ensureExists, ensureNonEmpty, read, readNonEmpty}

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.*
import fs2.Stream
import fs2.data.json.JsonException
import fs2.data.json.circe.*
import fs2.io.file.{Files, Path}
import io.circe.Decoder

import java.io.FileNotFoundException
import java.nio.file.Paths as JPaths
import scala.reflect.ClassTag

trait ArtifactReader[A: {ClassTag, Decoder}]:
  val artifactPath: String

  final def readAll: IO[List[A]] =
    openArtifact.ensureExists.read

  final def readNonEmpty: IO[NonEmptyList[A]] =
    openArtifact.ensureExists.ensureNonEmpty.readNonEmpty

  private def openArtifact =
    IO.blocking(
      Path.fromNioPath(JPaths.get(artifactPath)),
    )

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object ArtifactReader:
  def apply[A: {ClassTag, Decoder}](filename: String): ArtifactReader[A] =
    new ArtifactReader[A]:
      override val artifactPath: String = filename

  extension (self: IO[Path])
    def ensureExists: IO[Path] =
      self.flatTap: path =>
        Files[IO]
          .exists(path)
          .ensure(FileNotFoundException(s"❌ Artifact '${path.toString}' not found"))(identity)
          .void

    def ensureNonEmpty: IO[Path] =
      self.flatTap: path =>
        Files[IO]
          .size(path)
          .ensure(RuntimeException(s"❌ Artifact '${path.toString}' is empty"))(_ > 0L)
          .void

    def readNonEmpty[A: {ClassTag, Decoder}]: IO[NonEmptyList[A]] =
      self.flatMap: path =>
        IO.pure(path)
          .read
          .flatMap: items =>
            IO.fromOption(NonEmptyList.fromList(items))(
              RuntimeException(s"❌ Artifact '${path.toString}' contains no items"),
            )

    def read[A: {ClassTag, Decoder}]: IO[List[A]] =
      Stream
        .eval(self)
        .flatMap: path =>
          Files[IO]
            .readAll(path)
            .through(readJsonLines)
            .flatMap: json =>
              json.as[A] match
                case Right(value) => Stream.emit(value)
                case Left(error) =>
                  val clazz = summon[ClassTag[A]].runtimeClass
                  Stream.raiseError[IO](
                    JsonException(
                      s"Failed to decode '${clazz.getCanonicalName}' from '$json'",
                      inner = error,
                    ),
                  )
        .compile
        .toList

    private def readJsonLines(input: Stream[IO, Byte]) =
      input
        .through(fs2.text.utf8.decode)
        .through(fs2.text.lines)
        .flatMap: line =>
          Stream
            .emit(line)
            .covary[IO]
            .through(fs2.data.json.ast.parse)
            .handleErrorWith: error =>
              Stream.raiseError[IO](
                JsonException(s"'$line' is not a valid JSON value", inner = error),
              )
  end extension
