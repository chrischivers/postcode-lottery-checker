import sbt.Keys.test

name := "postcode-lottery-checker"

version := "1.0"

scalaVersion := "2.12.3"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.9"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "javax.mail" % "mail" % "1.5.0-b01"
libraryDependencies += "com.typesafe" % "config" % "1.3.1"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.5"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.8.1"
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0"
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.0.0-RC2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "com.xebialabs.restito" % "restito" % "0.9.1" % "test"
libraryDependencies += "net.sourceforge.htmlunit" % "htmlunit" % "2.27"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-lambda" % "1.11.160"
libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.1.0"
libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "1.3.0"
libraryDependencies += "com.github.cb372" % "scalacache-core_2.12" % "0.10.0"
libraryDependencies += "com.github.cb372" %% "scalacache-guava" % "0.10.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "0.4"
libraryDependencies += "com.github.etaty" %% "rediscala" % "1.8.0"
libraryDependencies += "org.mockito" % "mockito-core" % "2.11.0"


val circeVersion = "0.8.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

lazy val commonSettings = Seq(
  version := "1.0",
  organization := "com.postcodelotterychecker",
  scalaVersion := "2.11.11",
  test in assembly := {}
)

lazy val app = (project in file("app")).
  settings(commonSettings: _*).
  settings(
    mainClass in assembly := Some("com.postcodelotterychecker.Main"),
    test in assembly := {}
  )