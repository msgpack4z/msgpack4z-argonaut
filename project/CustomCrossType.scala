import sbt._
import org.scalajs.sbtplugin.cross.CrossType

object CustomCrossType extends CrossType {
  override def projectDir(crossBase: File, projectType: String) =
    crossBase / projectType

  def shared(projectBase: File, conf: String) =
    projectBase.getParentFile / "src" / conf / "scala"

  override def sharedSrcDir(projectBase: File, conf: String) =
    Some(shared(projectBase, conf))
}
