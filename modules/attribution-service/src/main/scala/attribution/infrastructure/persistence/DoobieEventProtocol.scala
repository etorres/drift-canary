package es.eriktorr
package attribution.infrastructure.persistence

import attribution.model.Event

import doobie.Meta

trait DoobieEventProtocol extends DoobieConversionInstanceProtocol:
  given Meta[Event.Source] =
    Meta[String].imap(Event.Source.valueOf)(_.toString)
