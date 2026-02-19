package es.eriktorr
package attribution.config

import attribution.model.Attribution.ModelVersion
import attribution.model.ConversionInstance.ConversionAction

import cats.implicits.catsSyntaxTuple3Semigroupal
import com.monovore.decline.Opts

final case class AttributionParams(
    defaultConversionAction: ConversionAction,
    modelVersion: ModelVersion,
    verbose: Boolean,
)

object AttributionParams extends ConversionActionArgument with ModelVersionArgument:
  def opts: Opts[AttributionParams] =
    (
      Opts
        .option[ConversionAction](
          long = "defaultConversionAction",
          help = "Set the default conversion action",
        )
        .withDefault(ConversionAction("purchase_prod")),
      Opts
        .option[ModelVersion](
          long = "modelVersion",
          help = "Set the model version (e.g v1, v2)",
        )
        .withDefault(ModelVersion.v1),
      Opts
        .flag("verbose", short = "v", help = "Print extra information to the logs.")
        .orFalse,
    ).mapN(AttributionParams.apply)
