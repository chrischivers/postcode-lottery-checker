name := "postcode-lottery-checker"

version := "1.0"

scalaVersion := "2.11.11"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.9"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "javax.mail" % "mail" % "1.5.0-b01"
libraryDependencies += "com.typesafe" % "config" % "1.3.1"
libraryDependencies += "com.github.scribejava" % "scribejava-core" % "4.1.1"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.3"
libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.5.3"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.5"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.8.1"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.3.0"
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.0.0-RC2"

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
    mainClass in assembly := Some("com.postcodelotterychecker.Main")
  )

lazy val utils = (project in file("utils")).
  settings(commonSettings: _*).
  settings(
    assemblyJarName in assembly := "utils.jar"
  )