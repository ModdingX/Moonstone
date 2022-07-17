package org.moddingx.moonstone.platform

import com.google.gson.JsonElement
import com.intellij.openapi.Disposable
import org.moddingx.moonstone.model.{FileEntry, Side}

import java.net.URI
import java.util.Locale

trait ModdingPlatform {
  
  val name: String
  
  def createAccess(list: ModList): PlatformAccess
  protected def validateVersion(file: FileEntry): Option[FileEntry] = Some(file)
  final def validateEntry(file: FileEntry): Option[FileEntry] = validateVersion(file).map(_.withSide(file.side).withLock(file.locked))
}

trait PlatformAccess extends Disposable {
  def projectName(project: JsonElement): String
  def projectDescription(project: JsonElement): String
  def projectLogo(project: JsonElement): Option[URI]
  def projectSite(project: JsonElement): Option[URI]
  def thirdPartyDownloads(project: JsonElement): Boolean
  def versionName(file: FileEntry): String
  
  def latestFile(project: JsonElement): Option[FileEntry]
  def allFiles(project: JsonElement): Seq[FileEntry]
  def latestFrom(files: Set[FileEntry]): Option[FileEntry]
  def searchMods(query: String): Seq[JsonElement]
  def dependencies(file: FileEntry): Seq[ResolvableDependency]
}

object ModdingPlatform {
  
  val CURSE: ModdingPlatform = ??? // TODO (replace liteloader with lite_loader when parsing modloader)
  val MODRINTH: ModdingPlatform = ??? // TODO
  
  val platforms: Seq[ModdingPlatform] = Seq(CURSE, MODRINTH)
  
  def get(str: String): ModdingPlatform = str.toLowerCase(Locale.ROOT) match {
    case MODRINTH.name => MODRINTH
    case _ => CURSE // Curse is default for historical reasons
  }
}

sealed trait ResolvableDependency {
  
  def project: JsonElement
  def file: Option[FileEntry]
  def side: Side // Side in file entry is ignored
}

case class FileDependency(entry: FileEntry, override val side: Side = Side.COMMON) extends ResolvableDependency {
  
  override val project: JsonElement = entry.project
  override val file: Option[FileEntry] = Some(entry)
}

case class ProjectDependency(override val project: JsonElement, list: ModList, override val side: Side = Side.COMMON) extends ResolvableDependency {
  
  override lazy val file: Option[FileEntry] = list.access.latestFile(project)
}
