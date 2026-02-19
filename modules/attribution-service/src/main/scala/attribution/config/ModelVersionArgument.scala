package es.eriktorr
package attribution.config

import attribution.model.Attribution.ModelVersion

import cats.data.ValidatedNel
import cats.implicits.*
import com.monovore.decline.Argument

import scala.util.Try

trait ModelVersionArgument:
  given modelVersionArgument: Argument[ModelVersion] =
    new Argument[ModelVersion]:
      override def read(string: String): ValidatedNel[String, ModelVersion] =
        Try(ModelVersion.valueOf(string)).toEither
          .leftMap(_.getMessage)
          .toValidatedNel

      override def defaultMetavar: String = "modelVersion"

object ModelVersionArgument extends ModelVersionArgument
