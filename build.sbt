ThisBuild / organization := "es.eriktorr"
ThisBuild / version := "1.0.0"
ThisBuild / idePackagePrefix := Some("es.eriktorr")
Global / excludeLintKeys += idePackagePrefix

ThisBuild / scalaVersion := "3.8.1"

ThisBuild / semanticdbEnabled := true
ThisBuild / javacOptions ++= Seq("-source", "25", "-target", "25")

Global / cancelable := true
Global / fork := true
Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias(
  "check",
  "; undeclaredCompileDependenciesTest; unusedCompileDependenciesTest; scalafixAll; scalafmtSbtCheck; scalafmtCheckAll",
)

lazy val contribWarts = Seq(
  ContribWart.MissingOverride,
  ContribWart.OldTime,
  ContribWart.SomeApply,
  ContribWart.UnsafeInheritance,
)
lazy val warts = Warts.unsafe.filter(_ != Wart.DefaultArguments) ++ contribWarts

lazy val Default = config("default").extend(Test)
lazy val Scheduled = config("scheduled").extend(Test)
lazy val Delayed = config("delayed").extend(Test)

lazy val withBaseSettings: Project => Project =
  _.configs(Default, Scheduled, Delayed)
    .settings(
      run / javaOptions += "--sun-misc-unsafe-memory-access=allow",
      Test / javaOptions += "--sun-misc-unsafe-memory-access=allow",
      tpolecatDevModeOptions ++= Set(
        org.typelevel.scalacoptions.ScalacOptions.javaOutputVersion("25"),
        org.typelevel.scalacoptions.ScalacOptions.warnError,
        org.typelevel.scalacoptions.ScalacOptions.warnSafeInit,
        org.typelevel.scalacoptions.ScalacOptions.privateExplicitNulls,
      ),
      tpolecatExcludeOptions ++= Set(
        org.typelevel.scalacoptions.ScalacOptions.fatalWarnings,
      ),
      Compile / compile / wartremoverErrors ++= warts,
      Test / compile / wartremoverErrors ++= warts,
      Compile / doc / sources := Seq(),
      Test / envVars := Map(
        "SBT_TEST_ENV_VARS" -> "true",
      ),
      testFrameworks += TestFrameworks.MUnit,
      inConfig(Default)(Defaults.testTasks),
      inConfig(Scheduled)(Defaults.testTasks),
      inConfig(Delayed)(Defaults.testTasks),
      Default / testOptions +=
        Tests.Argument(TestFrameworks.MUnit, "--exclude-tags=scheduled,delayed"),
      Scheduled / testOptions +=
        Tests.Argument(TestFrameworks.MUnit, "--include-tags=scheduled"),
      Delayed / testOptions +=
        Tests.Argument(TestFrameworks.MUnit, "--include-tags=delayed"),
      Test / logBuffered := false,
    )

lazy val withMunitCatsEffect: Project => Project =
  withBaseSettings.compose(
    _.settings(
      libraryDependencies ++= Seq(
        "com.47deg" %% "scalacheck-toolbox-datetime" % "0.7.0" % Test,
        "io.chrisdavenport" %% "cats-scalacheck" % "0.3.2" % Test,
        "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test,
        "org.typelevel" %% "scalacheck-effect-munit" % "2.1.0-RC1" % Test,
        "org.typelevel" %% "scalacheck-effect" % "2.1.0-RC1" % Test,
      ),
    ),
  )

lazy val attributionModel =
  (project in file("modules/attribution-model"))
    .configure(withMunitCatsEffect)
    .settings(
      name := "attribution-model",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core" % "0.14.15",
        "org.slf4j" % "slf4j-nop" % "1.7.36" % Test,
        "org.typelevel" %% "cats-collections-core" % "0.9.10",
        "org.typelevel" %% "cats-core" % "2.13.0",
        "org.typelevel" %% "cats-kernel" % "2.13.0",
        "org.typelevel" %% "cats-time" % "0.6.0",
        "org.typelevel" %% "kittens" % "3.5.0",
      ),
    )
    .dependsOn(testSupport % "compile->test")

lazy val attributionService =
  (project in file("modules/attribution-service"))
    .enablePlugins(DockerPlugin, JavaAppPackaging)
    .configure(withMunitCatsEffect)
    .settings(
      name := "attribution-service",
      libraryDependencies ++= Seq(
        "co.fs2" %% "fs2-io" % "3.12.2",
        "com.comcast" %% "ip4s-core" % "3.7.0",
        "com.lmax" % "disruptor" % "3.4.4" % Runtime,
        "com.monovore" %% "decline" % "2.6.0",
        "com.monovore" %% "decline-effect" % "2.6.0",
        "com.zaxxer" % "HikariCP" % "7.0.2" exclude ("org.slf4j", "slf4j-api"),
        "io.circe" %% "circe-core" % "0.14.15",
        "org.apache.logging.log4j" % "log4j-core" % "2.25.3" % Runtime,
        "org.apache.logging.log4j" % "log4j-layout-template-json" % "2.25.3" % Runtime,
        "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.25.3" % Runtime,
        "org.flywaydb" % "flyway-core" % "12.0.2",
        "org.flywaydb" % "flyway-database-postgresql" % "12.0.2",
        "org.http4s" %% "http4s-circe" % "0.23.33",
        "org.http4s" %% "http4s-core" % "0.23.33",
        "org.http4s" %% "http4s-dsl" % "0.23.33",
        "org.http4s" %% "http4s-ember-client" % "0.23.33" % Test,
        "org.http4s" %% "http4s-ember-server" % "0.23.33",
        "org.http4s" %% "http4s-server" % "0.23.33",
        "org.postgresql" % "postgresql" % "42.7.10" % Runtime,
        "org.tpolecat" %% "doobie-core" % "1.0.0-RC12",
        "org.tpolecat" %% "doobie-free" % "1.0.0-RC12",
        "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC12",
        "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC12",
        "org.tpolecat" %% "typename" % "1.1.0",
        "org.typelevel" %% "cats-collections-core" % "0.9.10",
        "org.typelevel" %% "cats-core" % "2.13.0",
        "org.typelevel" %% "cats-effect" % "3.6.3",
        "org.typelevel" %% "cats-effect-kernel" % "3.6.3",
        "org.typelevel" %% "cats-effect-std" % "3.6.3",
        "org.typelevel" %% "cats-free" % "2.13.0",
        "org.typelevel" %% "cats-kernel" % "2.13.0",
        "org.typelevel" %% "case-insensitive" % "1.5.0",
        "org.typelevel" %% "cats-time" % "0.6.0",
        "org.typelevel" %% "kittens" % "3.5.0",
        "org.typelevel" %% "log4cats-core" % "2.7.1",
        "org.typelevel" %% "log4cats-slf4j" % "2.7.1",
      ),
      dockerRepository := Some("ghcr.io/etorres"),
      dockerApiVersion := com.typesafe.sbt.packager.docker.DockerApiVersion.parse("1.53"),
      dockerBaseImage := "eclipse-temurin:25-jre",
      dockerExposedPorts ++= Seq(8080),
      Docker / maintainer := "https://github.com/etorres/drift-canary",
      Universal / javaOptions ++= Seq(
        "-J--sun-misc-unsafe-memory-access=allow",
      ),
    )
    .dependsOn(attributionModel % "compile->compile;test->test")
    .dependsOn(testSupport % "compile->test")

lazy val driftCanary =
  (project in file("modules/drift-canary"))
    .configure(withMunitCatsEffect)
    .settings(
      name := "drift-canary",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core" % "0.14.15",
        "org.http4s" %% "http4s-circe" % "0.23.33" % Test,
        "org.http4s" %% "http4s-ember-client" % "0.23.33" % Test,
        "org.typelevel" %% "cats-core" % "2.13.0",
        "org.typelevel" %% "cats-kernel" % "2.13.0",
        "org.typelevel" %% "cats-time" % "0.6.0",
        "org.typelevel" %% "kittens" % "3.5.0",
      ),
    )
    .dependsOn(attributionModel % "compile->compile;test->test")
    .dependsOn(testSupport % "compile->test")

lazy val testSupport =
  (project in file("modules/test-support"))
    .configure(withBaseSettings)
    .settings(
      name := "test-support",
      libraryDependencies ++= Seq(
        "com.47deg" %% "scalacheck-toolbox-datetime" % "0.7.0",
        "org.scalacheck" %% "scalacheck" % "1.19.0",
        "org.scalameta" %% "munit" % "1.2.2",
        "org.typelevel" %% "cats-collections-core" % "0.9.10",
        "org.typelevel" %% "cats-effect" % "3.6.3",
        "org.typelevel" %% "cats-effect-kernel" % "3.6.3",
        "org.typelevel" %% "cats-effect-std" % "3.6.3",
      ),
    )

lazy val root =
  (project in file("."))
    .aggregate(
      attributionModel,
      attributionService,
      driftCanary,
      testSupport,
    )
    .configure(withBaseSettings)
    .settings(
      name := "drift-canary",
      publish := {},
      publishLocal := {},
    )
