val scala3Version = "3.0.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-callgraph",
    version := "0.1.0",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",

      "org.apache.bcel" % "bcel" % "6.5.0",
      "org.javassist" % "javassist" % "3.28.0-GA",

      "org.typelevel" %% "cats-effect" % "3.3.11",

      "org.scalatest" %% "scalatest" % "3.2.11" % Test
//      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0"
    )
  )
