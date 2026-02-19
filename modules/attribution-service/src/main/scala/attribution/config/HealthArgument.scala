package es.eriktorr
package attribution.config

import attribution.config.HealthConfig.{LivenessPath, ReadinessPath, ServiceName}

import cats.data.ValidatedNel
import cats.implicits.*
import com.monovore.decline.Argument

trait HealthArgument:
  given Argument[LivenessPath] = new Argument[LivenessPath]:
    override def read(string: String): ValidatedNel[String, LivenessPath] =
      LivenessPath
        .fromString(string)
        .leftMap(_.getMessage)
        .toValidatedNel
    override def defaultMetavar: String = "livenessPath"

  given Argument[ReadinessPath] = new Argument[ReadinessPath]:
    override def read(string: String): ValidatedNel[String, ReadinessPath] =
      ReadinessPath
        .fromString(string)
        .leftMap(_.getMessage)
        .toValidatedNel
    override def defaultMetavar: String = "readinessPath"

  given Argument[ServiceName] = new Argument[ServiceName]:
    override def read(string: String): ValidatedNel[String, ServiceName] =
      ServiceName
        .fromString(string)
        .leftMap(_.getMessage)
        .toValidatedNel
    override def defaultMetavar: String = "serviceName"

object HealthArgument extends HealthArgument
