package es.eriktorr
package attribution.infrastructure

import cats.effect.IO
import fs2.Stream
import fs2.data.json.circe.*
import fs2.io.file.{Files, Path}
import io.circe.syntax.given
import io.circe.{Encoder, Json}

import java.nio.file.Paths as JPaths

abstract class ArtifactWriter[A: Encoder]:
  val artifactPath: String

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  final def write(items: Seq[A]): IO[Unit] =
    val path = Path.fromNioPath(JPaths.get(artifactPath))
    val jsonStream = Stream.emits(items).covary[IO].map(_.asJson)
    writeJsonLines(jsonStream)
      .through(
        Files[IO].writeAll(path),
      )
      .compile
      .drain

  private def writeJsonLines(
      input: Stream[IO, Json],
  ) =
    input
      .evalMap { json =>
        Stream
          .emit(json)
          .covary[IO]
          .through(fs2.data.json.ast.tokenize)
          .through(fs2.data.json.render.compact)
          .compile
          .string
      }
      .intersperse("\n")
      .through(fs2.text.utf8.encode)

object ArtifactWriter:
  def apply[A: Encoder](filename: String): ArtifactWriter[A] =
    new ArtifactWriter[A]:
      override val artifactPath: String = filename
