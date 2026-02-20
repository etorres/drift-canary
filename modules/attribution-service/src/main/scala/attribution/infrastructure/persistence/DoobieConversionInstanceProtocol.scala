package es.eriktorr
package attribution.infrastructure.persistence

import attribution.model.ConversionInstance.{ConversionAction, EventId}

import cats.implicits.*
import doobie.Meta

trait DoobieConversionInstanceProtocol:
  given Meta[ConversionAction] =
    Meta[String].tiemap { value =>
      ConversionAction.fromString(value).leftMap(_.getMessage)
    }(identity)

  given Meta[EventId] =
    Meta[String].tiemap { value =>
      EventId.fromString(value).leftMap(_.getMessage)
    }(identity)
