import Dependencies._

scalaVersion := "2.11.5"
name := "list-extraction"
version := "0.1"
organization := "edu.iitd"

enablePlugins(CliPlugin, DeployPlugin)

libraryDependencies ++= Seq(
  allenAiCommon,
  nlpstackModule("tokenize"),
  nlpstackModule("postag"),
  nlpstackModule("chunk"),
  nlpstackModule("lemmatize"),
  nlpstackModule("parse"),
  nlpstackModule("segment"),
  unitTesting,
  word2vec
)

javaOptions += "-Xmx4G"

conflictManager := ConflictManager.default

dependencyOverrides ++= Set(
  allenAiCommon,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "commons-codec" % "commons-codec" % "1.6",
  "org.apache.commons" % "commons-compress" % "1.8",
  "org.scala-lang" % "scala-reflect" % "2.11.5"
)