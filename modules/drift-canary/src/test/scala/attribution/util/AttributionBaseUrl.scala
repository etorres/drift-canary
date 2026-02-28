package es.eriktorr
package attribution.util

import cats.effect.IO
import cats.effect.std.Env
import org.http4s.Uri
import cats.implicits.*

object AttributionBaseUrl:
  def getBaseUrl: IO[Uri] =
    Env[IO]
      .get("ATTRIBUTION_BASE_URL")
      .flatMap: maybeBaseUrl =>
        val baseUrl = maybeBaseUrl.getOrElse("http://localhost:8080")
        IO.fromEither(
          Uri
            .fromString(baseUrl)
            .map(_.addPath("api/v1"))
            .leftMap: parseFailure =>
              IllegalArgumentException(
                show"Invalid ATTRIBUTION_BASE_URL: $baseUrl",
                parseFailure,
              ),
        )
