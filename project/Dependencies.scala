import org.allenai.plugins.CoreDependencies

import sbt._

object Dependencies extends CoreDependencies {
  override val allenAiCommon = "org.allenai.common" %% "common-core" % "1.0.1"
  val codeLogging = Seq("org.slf4j" % "slf4j-api" % "1.7.5", "org.slf4j" % "slf4j-simple" % "1.7.5",
    "org.clapper" %% "grizzled-slf4j" % "1.0.1")
  val unitTesting =  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  val word2vec = "com.medallia.word2vec" % "Word2VecJava" % "0.10.3"
  val nlpstackVersion = "1.10"
  def nlpstackModule(id: String) = "org.allenai.nlpstack" %% s"nlpstack-${id}" % nlpstackVersion
}
