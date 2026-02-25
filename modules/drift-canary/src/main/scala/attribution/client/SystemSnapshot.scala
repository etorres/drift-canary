package es.eriktorr
package attribution.client

import attribution.model.{Attribution, Event}

import cats.derived.*
import cats.{Eq, Show}
import io.circe.Codec

final case class SystemSnapshot(
    events: List[Event],
    attributions: List[Attribution],
) derives Eq,
      Show,
      Codec
