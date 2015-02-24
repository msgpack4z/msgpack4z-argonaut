import sbt._, Keys._

object build extends Build {

  private val msgpack4zArgonautName = "msgpack4z-argonaut"
  val modules = msgpack4zArgonautName :: Nil

  lazy val msgpack4z = Project("msgpack4z-argonaut", file(".")).settings(
    Common.settings: _*
  ).settings(
    name := msgpack4zArgonautName,
    libraryDependencies ++= (
      ("io.argonaut" %% "argonaut" % "6.1-M5" exclude("org.scala-lang", "scala-compiler")) ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.1.1") ::
      ("org.scalacheck" %% "scalacheck" % "1.12.2" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java07" % "0.1.2" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.1.0" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.1.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
