package es.eriktorr
package attribution.infrastructure

import io.circe.{Decoder, Encoder}

import scala.reflect.ClassTag

object ArtifactStore:
  type ArtifactStore[A] = ArtifactReader[A] & ArtifactWriter[A]

  def apply[A: {ClassTag, Decoder, Encoder}](
      filename: String,
  ): ArtifactStore[A] =
    new ArtifactReader[A] with ArtifactWriter[A]:
      override val artifactPath: String = filename
