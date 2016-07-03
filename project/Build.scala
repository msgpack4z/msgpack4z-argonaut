import sbt._, Keys._
import scalaprops.ScalapropsPlugin.autoImport._

object build extends Build {

  private val msgpack4zArgonautName = "msgpack4z-argonaut"
  val modules = msgpack4zArgonautName :: Nil

  lazy val msgpack4z = Project("msgpack4z-argonaut", file(".")).settings(
    Common.settings ++ scalapropsSettings
  ).settings(
    name := msgpack4zArgonautName,
    scalapropsVersion := "0.3.2",
    libraryDependencies ++= (
      ("io.argonaut" %% "argonaut" % "6.1a") ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.3.3") ::
      ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.3" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
