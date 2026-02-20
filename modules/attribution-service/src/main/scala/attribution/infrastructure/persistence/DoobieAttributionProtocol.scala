package es.eriktorr
package attribution.infrastructure.persistence

import attribution.model.Attribution

import doobie.Meta

trait DoobieAttributionProtocol extends DoobieConversionInstanceProtocol:
  given Meta[Attribution.Channel] =
    Meta[String].imap(Attribution.Channel.valueOf)(_.toString)

  given Meta[Attribution.ModelVersion] =
    Meta[String].imap(Attribution.ModelVersion.valueOf)(_.toString)
