import build._
import org.scalajs.sbtplugin.cross.CrossProject

val argonautVersion = "6.2"

val msgpack4zArgonaut = CrossProject(
  msgpack4zArgonautName,
  file("."),
  CustomCrossType
).settings(
  Common.settings,
  scalapropsCoreSettings,
  name := msgpack4zArgonautName,
  libraryDependencies ++= (
    ("io.argonaut" %%% "argonaut" % argonautVersion) ::
    ("io.argonaut" %%% "argonaut-scalaz" % argonautVersion % "test") ::
    ("com.github.scalaprops" %%% "scalaprops" % "0.4.2" % "test") ::
    ("com.github.xuwei-k" %%% "msgpack4z-core" % "0.3.7") ::
    Nil
  )
).jvmSettings(
  libraryDependencies ++= (
    ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.3" % "test") ::
    ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.5" % "test") ::
    ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
    Nil
  ),
  Sxr.settings
).jsSettings(
  scalaJSOptimizerOptions ~= { options =>
    // https://github.com/scala-js/scala-js/issues/2798
    try {
      scala.util.Properties.isJavaAtLeast("1.8")
      options
    } catch {
      case _: NumberFormatException =>
        options.withParallel(false)
    }
  },
  scalacOptions += {
    val a = (baseDirectory in LocalRootProject).value.toURI.toString
    val g = "https://raw.githubusercontent.com/msgpack4z/msgpack4z-argonaut/" + Common.tagOrHash.value
    s"-P:scalajs:mapSourceURI:$a->$g/"
  },
  scalaJSSemantics ~= { _.withStrictFloats(true) }
)

val msgpack4zArgonautJVM = msgpack4zArgonaut.jvm
val msgpack4zArgonautJS = msgpack4zArgonaut.js

val root = Project("root", file(".")).settings(
  Common.settings,
  commands += Command.command("testSequential"){
    Seq(msgpack4zArgonautJVM, msgpack4zArgonautJS).map(_.id + "/test") ::: _
  },
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  publishArtifact in Compile := false,
  scalaSource in Compile := file("dummy"),
  scalaSource in Test := file("dummy")
).aggregate(
  msgpack4zArgonautJS, msgpack4zArgonautJVM
)
