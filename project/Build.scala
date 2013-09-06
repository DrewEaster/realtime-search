import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "realtime-search"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.elasticsearch" % "elasticsearch" % "0.90.1",
    "commons-io" % "commons-io" % "2.4",
    "org.webjars" %% "webjars-play" % "2.1.0-3",
    "org.webjars" % "angularjs" % "1.1.5-1",
    "org.webjars" % "bootstrap" % "2.3.2"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )
}
