import build._
import sbtcrossproject.CrossProject

val argonautVersion = "6.2.3"

val msgpack4zArgonaut = CrossProject(
  id = msgpack4zArgonautName,
  base = file(".")
)(
  JSPlatform, JVMPlatform, NativePlatform
).crossType(
  CustomCrossType
).settings(
  Common.settings,
  scalapropsCoreSettings,
  name := msgpack4zArgonautName,
  libraryDependencies ++= (
    ("io.argonaut" %%% "argonaut" % argonautVersion) ::
    ("io.argonaut" %%% "argonaut-scalaz" % argonautVersion % "test") ::
    ("com.github.scalaprops" %%% "scalaprops" % "0.6.2" % "test") ::
    ("com.github.xuwei-k" %%% "msgpack4z-core" % "0.3.11") ::
    Nil
  )
).jvmSettings(
  libraryDependencies ++= (
    ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.5" % "test") ::
    ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.6" % "test") ::
    ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
    Nil
  ),
  Sxr.settings
).jsSettings(
  scalacOptions += {
    val a = (baseDirectory in LocalRootProject).value.toURI.toString
    val g = "https://raw.githubusercontent.com/msgpack4z/msgpack4z-argonaut/" + Common.tagOrHash.value
    s"-P:scalajs:mapSourceURI:$a->$g/"
  },
  scalaJSSemantics ~= { _.withStrictFloats(true) }
).nativeSettings(
  scalapropsNativeSettings,
  test in Test := {}, // TODO scala.Float roundtrip fail
  scalaVersion := Common.Scala211
)

val msgpack4zArgonautJVM = msgpack4zArgonaut.jvm
val msgpack4zArgonautJS = msgpack4zArgonaut.js
val msgpack4zArgonautNative = msgpack4zArgonaut.native

val root = Project("root", file(".")).settings(
  Common.settings,
  commands += Command.command("testSequential"){
    List(msgpack4zArgonautJVM, msgpack4zArgonautJS).map(_.id + "/test") ::: _
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
