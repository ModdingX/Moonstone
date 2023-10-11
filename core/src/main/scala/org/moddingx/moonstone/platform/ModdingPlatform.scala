package org.moddingx.moonstone.platform

import com.google.gson.JsonElement
import org.moddingx.moonstone.logic.Destroyable
import org.moddingx.moonstone.model.{FileEntry, Side}
import org.moddingx.moonstone.platform.curse.CursePlatform
import org.moddingx.moonstone.platform.modrinth.ModrinthPlatform

import java.net.URI
import java.util.Locale

trait ModdingPlatform {
  
  val name: String
  
  def createAccess(list: ModList): PlatformAccess
  protected def validateVersion(file: FileEntry): Option[FileEntry] = Some(file)
  final def validateEntry(file: FileEntry): Option[FileEntry] = validateVersion(file).map(_.withSide(file.side).withLock(file.locked))
}

trait PlatformAccess extends Destroyable {
  def projectName(project: JsonElement): String
  def projectDescription(project: JsonElement): String
  def projectLogo(project: JsonElement): Option[URI]
  def projectSite(project: JsonElement): Option[URI]
  def thirdPartyDownloads(project: JsonElement): Boolean
  def defaultFileSide(file: FileEntry): Side
  def versionName(file: FileEntry): String
  def versionByInput(file: FileEntry, input: String): Option[FileEntry]

  // platform can load these into cache at once to make load time faster
  def modPackHint(files: Set[FileEntry]): Unit
  def latestFile(project: JsonElement): Option[FileEntry]
  def allFiles(project: JsonElement): Seq[FileEntry]
  def latestFrom(files: Set[FileEntry]): Option[FileEntry]
  def searchMods(query: String): Seq[JsonElement]
  def dependencies(file: FileEntry): Seq[ResolvableDependency]
  
  def metadataChange(): Unit
}

object ModdingPlatform {
  
  val CURSE: ModdingPlatform = CursePlatform
  val MODRINTH: ModdingPlatform = ModrinthPlatform
  
  val platforms: Seq[ModdingPlatform] = Seq(CURSE, MODRINTH)
  
  def get(str: String): ModdingPlatform = str.toLowerCase(Locale.ROOT) match {
    case MODRINTH.name => MODRINTH
    case _ => CURSE // Curse is default for historical reasons
  }
}

sealed trait ResolvableDependency {
  
  def modList: ModList
  def project: JsonElement
  def file: Option[FileEntry]
  def side: Side // Side in file entry is ignored
}

case class FileDependency(entry: FileEntry, override val modList: ModList, override val side: Side = Side.COMMON) extends ResolvableDependency {
  
  override val project: JsonElement = entry.project
  override val file: Option[FileEntry] = Some(entry)
}

case class ProjectDependency(override val project: JsonElement, override val modList: ModList, override val side: Side = Side.COMMON) extends ResolvableDependency {
  
  override lazy val file: Option[FileEntry] = modList.access.latestFile(project)
}
