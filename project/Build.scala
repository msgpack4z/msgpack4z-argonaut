import sbt._, Keys._
import scalaprops.ScalapropsPlugin.autoImport._

object build extends Build {

  private val msgpack4zArgonautName = "msgpack4z-argonaut"
  val modules = msgpack4zArgonautName :: Nil

  lazy val msgpack4z = Project("msgpack4z-argonaut", file(".")).settings(
    Common.settings ++ scalapropsSettings
  ).settings(
    name := msgpack4zArgonautName,
    scalapropsVersion := "0.1.16",
    libraryDependencies ++= (
      ("io.argonaut" %% "argonaut" % "6.1") ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.2.0") ::
      ("com.github.xuwei-k" % "msgpack4z-java07" % "0.2.0" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.2.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
