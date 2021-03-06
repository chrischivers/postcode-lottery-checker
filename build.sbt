import sbt.Keys.test

name := "postcode-lottery-checker"

version := "1.0"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.9",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "javax.mail" % "mail" % "1.5.0-b01",
  "com.typesafe" % "config" % "1.3.1",
  "org.apache.commons" % "commons-lang3" % "3.5",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.8.1",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "net.ruippeixotog" %% "scala-scraper" % "2.0.0-RC2",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.xebialabs.restito" % "restito" % "0.9.1" % "test",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.27",
  "com.amazonaws" % "aws-java-sdk-lambda" % "1.11.160",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
  "com.github.cb372" % "scalacache-core_2.12" % "0.10.0",
  "com.github.cb372" %% "scalacache-guava" % "0.10.0",
  "org.typelevel" %% "cats-effect" % "0.4",
  "com.github.etaty" %% "rediscala" % "1.8.0",
  "com.github.mauricio" % "postgresql-async_2.12" % "0.2.21",
  "org.mockito" % "mockito-core" % "2.11.0",
  "commons-validator" % "commons-validator" % "1.6",
  "com.internetitem" % "logback-elasticsearch-appender" % "1.6"
)


val circeVersion = "0.9.0-M1"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val http4sVersion = "0.18.0-M1"
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl",
  "org.http4s" %% "http4s-blaze-server",
  "org.http4s" %% "http4s-blaze-client",
  "org.http4s" %% "http4s-circe"
).map(_ % http4sVersion)


mainClass in assembly := Some("com.postcodelotterychecker.Main")
test in assembly := {}
parallelExecution in Test := false

//lazy val commonSettings = Seq(
//  version := "1.0",
//  organization := "com.postcodelotterychecker",
//  scalaVersion := "2.11.11",
//  test in assembly := {}
//)

//lazy val app = (project in file("app")).
//  settings(commonSettings: _*).
//  settings(
//    mainClass in assembly := Some("com.postcodelotterychecker.Main"),
//    test in assembly := {}
//  )