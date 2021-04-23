import build._
import sbtcrossproject.CrossProject
import Common.isScala3

val argonautVersion = "6.3.3"

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
    ("com.github.scalaprops" %%% "scalaprops" % "0.8.2" % "test") ::
    ("com.github.xuwei-k" %%% "msgpack4z-core" % "0.5.1") ::
    Nil
  ).map(_ cross CrossVersion.for3Use2_13)
).jvmSettings(
  libraryDependencies ++= (
    ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.7" % "test" cross CrossVersion.for3Use2_13) ::
    ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.6" % "test") ::
    ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
    Nil
  ),
  Sxr.settings
).jsSettings(
  scalacOptions ++= {
    val a = (LocalRootProject / baseDirectory).value.toURI.toString
    val g = "https://raw.githubusercontent.com/msgpack4z/msgpack4z-argonaut/" + Common.tagOrHash.value
    if (isScala3.value) {
      Seq(s"-scalajs-mapSourceURI:$a->$g/")
    } else {
      Seq(s"-P:scalajs:mapSourceURI:$a->$g/")
    }
  },
  scalaJSLinkerConfig ~= { _.withSemantics(_.withStrictFloats(true)) }
).nativeSettings(
  scalapropsNativeSettings,
  crossScalaVersions ~= (_.filter(_ startsWith "2.1")),
)

val msgpack4zArgonautJVM = msgpack4zArgonaut.jvm
val msgpack4zArgonautJS = msgpack4zArgonaut.js
val msgpack4zArgonautNative = msgpack4zArgonaut.native

val root = Project("root", file(".")).settings(
  Common.settings,
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  Compile / publishArtifact := false,
  Compile / scalaSource := baseDirectory.value / "dummy",
  Test / scalaSource := baseDirectory.value / "dummy"
).aggregate(
  msgpack4zArgonautJS, msgpack4zArgonautJVM
)
