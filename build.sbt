// SPDX-License-Identifier: MIT
// Copyright (c) 2020 Hadrien Chauvin

name := "exploratory-pipeline-example"
organization in ThisBuild := "io.hchauvin"
scalaVersion in ThisBuild := "2.12.1"
version in ThisBuild := "0.1"
ThisBuild / fork in Test := true

// Get configuration for R.
lazy val rConfig = {
  import sys.process._

  val stdout = Seq("Rscript", "-e", "cat(system.file(\"jri\",package=\"rJava\"), \"|\", R.home())")!!
  val parts = stdout.split(" \\| ")

  println("JRI PATH: " + parts(0).trim)
  Map('jriPath -> parts(0).trim, 'rHome -> parts(1).trim)
}
ThisBuild / javaOptions in Test += s"-Djava.library.path=${rConfig('jriPath)}"
ThisBuild / envVars in Test := Map("R_HOME" -> rConfig('rHome))

lazy val global = project
  .in(file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    `spark-r`,
    `spark-reflow`,
    `bioinformatics`
  )

lazy val `spark-r` = project
  .settings(
    name := "spark-r",
    libraryDependencies ++= Seq(
      "org.nuiton.thirdparty" % "JRI" % "0.9-9",
      "org.apache.spark" %% "spark-core" % "3.0.0-preview2" % "provided",
      "org.apache.spark" %% "spark-sql" % "3.0.0-preview2" % "provided",
      "org.polynote" %% "polynote-runtime" % "0.3.4" % "provided",
      "org.scalatest" %% "scalatest" % "3.1.1" % "test",
      "com.holdenkarau" %% "spark-testing-base" % "2.4.5_0.14.0" % "test"
    ),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    test in assembly := {}
  )

lazy val `spark-reflow` = project
    .settings(
      name := "spark-reflow",
      libraryDependencies ++= Seq(
        "org.apache.spark" %% "spark-core" % "3.0.0-preview2" % "provided",
        "org.apache.spark" %% "spark-sql" % "3.0.0-preview2" % "provided",
        "org.json4s" %% "json4s-native" % "3.7.0-M2",
        "org.apache.hadoop" % "hadoop-aws" % "2.7.4",
        "org.scalatest" %% "scalatest" % "3.1.1" % "test",
        "com.holdenkarau" %% "spark-testing-base" % "2.4.5_0.14.0" % "test"
      ),
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
      test in assembly := {},
      assemblyMergeStrategy in assembly := {
        case PathList("META-INF", xs @ _*) => MergeStrategy.discard
        case x => MergeStrategy.first
      }
    )

lazy val `bioinformatics` = project
  .settings(
    name := "bioinformatics",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.0.0-preview2" % "provided",
      "org.apache.spark" %% "spark-sql" % "3.0.0-preview2" % "provided",
      "org.scalatest" %% "scalatest" % "3.1.1" % "test",
      "org.nuiton.thirdparty" % "JRI" % "0.9-9",
      "com.holdenkarau" %% "spark-testing-base" % "2.4.5_0.14.0" % "test"
    ),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  ).dependsOn(
    `spark-r` % "provided",
    `spark-reflow` % "provided"
  )

ThisBuild / assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

ThisBuild / test in assembly := {}