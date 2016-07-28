import build._

Common.settings

scalapropsSettings

name := msgpack4zArgonautName

scalapropsVersion := "0.3.3"

libraryDependencies ++= (
  ("io.argonaut" %% "argonaut" % "6.1a") ::
  ("com.github.xuwei-k" %% "msgpack4z-core" % "0.3.3") ::
  ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.4" % "test") ::
  ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
  ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.0" % "test") ::
  Nil
)

Sxr.subProjectSxr(Compile, "classes.sxr")
