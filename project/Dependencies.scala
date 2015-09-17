import org.allenai.plugins.CoreDependencies

import sbt._

object Dependencies extends CoreDependencies {
  override val allenAiCommon = "org.allenai.common" %% "common-core" % "1.0.1"
  val nlpstackVersion = "1.10"
  def nlpstackModule(id: String) = "org.allenai.nlpstack" %% s"nlpstack-${id}" % nlpstackVersion
}
