name := "postcode-lottery-checker"

version := "1.0"

scalaVersion := "2.11.11"

libraryDependencies += "com.google.oauth-client" % "google-oauth-client-java6" % "1.22.0"
libraryDependencies += "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0"
libraryDependencies += "com.google.api-client" % "google-api-client" % "1.22.0"
libraryDependencies += "com.google.apis" % "google-api-services-gmail" % "v1-rev65-1.22.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.9"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "javax.mail" % "mail" % "1.5.0-b01"
libraryDependencies += "com.typesafe" % "config" % "1.3.1"


val circeVersion = "0.8.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)