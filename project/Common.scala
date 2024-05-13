import sbt._, Keys._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.autoImport._
import com.jsuereth.sbtpgp.SbtPgp.autoImport.PgpKeys
import xerial.sbt.Sonatype.autoImport.sonatypePublishToBundle

object Common {

  val isScala3 = Def.setting(
    CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3)
  )

  val tagName = Def.setting{
    s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
  }
  val tagOrHash = Def.setting{
    if(isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head else tagName.value
  }

  private[this] val unusedWarnings = (
    "-Ywarn-unused" ::
    Nil
  )

  val Scala212 = "2.12.19"
  private[this] val Scala213 = "2.13.14"
  private[this] val Scala3 = "3.4.2"

  val settings = Seq(
    ReleasePlugin.extraReleaseCommands
  ).flatten ++ Seq(
    publishTo := sonatypePublishToBundle.value,
    fullResolvers ~= {_.filterNot(_.name == "jcenter")},
    commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask),
    releaseTagName := tagName.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      UpdateReadme.updateReadmeProcess,
      tagRelease,
      ReleaseStep(
        action = { state =>
          val extracted = Project extract state
          extracted.runAggregated(extracted.get(thisProjectRef) / (Global / PgpKeys.publishSigned), state)
        },
        enableCrossBuild = true
      ),
      releaseStepCommandAndRemaining("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
      UpdateReadme.updateReadmeProcess,
      pushChanges
    ),
    credentials ++= PartialFunction.condOpt(sys.env.get("SONATYPE_USER") -> sys.env.get("SONATYPE_PASS")){
      case (Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
    }.toList,
    organization := "com.github.xuwei-k",
    homepage := Some(url("https://github.com/msgpack4z")),
    licenses := Seq("MIT License" -> url("https://opensource.org/license/mit")),
    scalacOptions ++= (
      "-deprecation" ::
      "-unchecked" ::
      "-language:existentials" ::
      "-language:higherKinds" ::
      "-language:implicitConversions" ::
      Nil
    ),
    scalacOptions ++= {
      if (isScala3.value) {
        Nil
      } else {
        unusedWarnings ++ Seq(
          "-Xlint",
        )
      }
    },
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq(
            "-Xfuture",
            "-Yno-adapted-args"
          )
        case _ =>
          Seq.empty
      }
    },
    scalaVersion := Scala212,
    crossScalaVersions := Scala212 :: Scala213 :: Scala3 :: Nil,
    (Compile / doc / scalacOptions) ++= {
      val tag = tagOrHash.value
      if (isScala3.value) {
        Nil
      } else {
        Seq(
          "-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath,
          "-doc-source-url", s"https://github.com/msgpack4z/msgpack4z-argonaut/tree/${tag}€{FILE_PATH}.scala"
        )
      }
    },
    pomExtra :=
      <developers>
        <developer>
          <id>xuwei-k</id>
          <name>Kenji Yoshida</name>
          <url>https://github.com/xuwei-k</url>
        </developer>
      </developers>
      <scm>
        <url>git@github.com:msgpack4z/msgpack4z-argonaut.git</url>
        <connection>scm:git:git@github.com:msgpack4z/msgpack4z-argonaut.git</connection>
        <tag>{tagOrHash.value}</tag>
      </scm>
    ,
    description := "msgpack4z argonaut binding",
    pomPostProcess := { node =>
      import scala.xml._
      import scala.xml.transform._
      def stripIf(f: Node => Boolean) = new RewriteRule {
        override def transform(n: Node) =
          if (f(n)) NodeSeq.Empty else n
      }
      val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
      new RuleTransformer(stripTestScope).transform(node)(0)
    }
  ) ++ Seq(Compile, Test).flatMap(c =>
    c / console / scalacOptions --= unusedWarnings
  )

}
