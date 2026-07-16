import build._
import Common.isScala3

val argonautVersion = "6.3.13"

val scalaVersions = Seq(
  "2.12.21",
  "2.13.18",
  "3.3.8",
)

val msgpack4zArgonaut = projectMatrix
  .defaultAxes()
  .in(file("."))
  .withId(
    msgpack4zArgonautName
  )
  .settings(
    Common.settings,
    scalapropsCoreSettings,
    name := msgpack4zArgonautName,
    libraryDependencies ++= Seq(
      "io.github.argonaut-io" %% "argonaut" % argonautVersion,
      "io.github.argonaut-io" %% "argonaut-scalaz" % argonautVersion % "test",
      "com.github.scalaprops" %% "scalaprops" % "0.11.0" % "test",
      "com.github.xuwei-k" %% "msgpack4z-core" % "0.6.2",
    )
  )
  .jvmPlatform(
    scalaVersions,
    Def.settings(
      libraryDependencies ++= Seq(
        "com.github.xuwei-k" %% "msgpack4z-native" % "0.4.0" % "test",
        "com.github.xuwei-k" % "msgpack4z-java" % "0.4.0" % "test",
        "com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test",
      ),
    )
  )
  .jsPlatform(
    scalaVersions,
    Def.settings(
      scalacOptions ++= {
        val a = (LocalRootProject / baseDirectory).value.toURI.toString
        val g = "https://raw.githubusercontent.com/msgpack4z/msgpack4z-argonaut/" + Common.tagOrHash.value
        if (isScala3.value) {
          Seq(s"-scalajs-mapSourceURI:$a->$g/")
        } else {
          Seq(s"-P:scalajs:mapSourceURI:$a->$g/")
        }
      },
    )
  )
  .nativePlatform(
    scalaVersions,
    Def.settings(
      scalapropsNativeSettings,
    )
  )

val msgpack4zArgonautRoot = rootProject.autoAggregate.settings(
  Common.settings,
  autoScalaLibrary := false,
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  Compile / publishArtifact := false,
  Compile / scalaSource := baseDirectory.value / "dummy",
  Test / scalaSource := baseDirectory.value / "dummy",
  TaskKey[Unit]("testSequential") := Def
    .sequential(
      msgpack4zArgonaut
        .allProjects()
        .map(_._1)
        .sortBy(_.id)
        .flatMap(p =>
          Seq[Def.Initialize[Task[Unit]]](
            Def.task(streams.value.log.info(s"start ${p.id} test")),
            (p / Test / testFull).map(_ => ())
          )
        )
    )
    .value,
)
