import build._
import sbtcrossproject.CrossProject
import Common.isScala3

val argonautVersion = "6.3.8"

def Scala3 = "3.1.3"

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
    ("com.github.scalaprops" %%% "scalaprops" % "0.9.0" % "test") ::
    ("com.github.xuwei-k" %%% "msgpack4z-core" % "0.6.0") ::
    Nil
  )
).jvmSettings(
  libraryDependencies ++= (
    ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.8" % "test") ::
    ("com.github.xuwei-k" % "msgpack4z-java" % "0.4.0" % "test") ::
    ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
    Nil
  ),
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
).platformsSettings(JVMPlatform, JSPlatform)(
  crossScalaVersions += Scala3,
).nativeSettings(
  scalapropsNativeSettings,
  Compile / doc / scalacOptions --= {
    // TODO remove this workaround
    // https://github.com/scala-native/scala-native/issues/2503
    if (scalaBinaryVersion.value == "3") {
      (Compile / doc / scalacOptions).value.filter(_.contains("-Xplugin"))
    } else {
      Nil
    }
  },
  crossScalaVersions += "3.1.0",
)

val msgpack4zArgonautJVM = msgpack4zArgonaut.jvm
val msgpack4zArgonautJS = msgpack4zArgonaut.js
val msgpack4zArgonautNative = msgpack4zArgonaut.native

val root = Project("root", file(".")).settings(
  Common.settings,
  crossScalaVersions += Scala3,
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
