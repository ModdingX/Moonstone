package org.moddingx.moonstone.platform

import com.google.gson.JsonElement
import org.moddingx.moonstone.model.{FileEntry, Side}

import java.net.URI

class WrappedAccess(access: PlatformAccess) extends PlatformAccess {
  
  override def projectName(project: JsonElement): String = wrap("project-" + project) { access.projectName(project) }
  override def projectDescription(project: JsonElement): String = wrap("") { access.projectDescription(project) }
  override def projectLogo(project: JsonElement): Option[URI] = opt { access.projectLogo(project) }
  override def projectSite(project: JsonElement): Option[URI] = opt { access.projectSite(project) }
  override def thirdPartyDownloads(project: JsonElement): Boolean = wrap(true) { access.thirdPartyDownloads(project) }
  override def defaultFileSide(file: FileEntry): Side = wrap[Side](Side.COMMON) { access.defaultFileSide(file) }
  override def versionName(file: FileEntry): String = wrap("version-" + file.file) { access.versionName(file) }
  override def versionByInput(file: FileEntry, input: String): Option[FileEntry] = opt { access.versionByInput(file, input) }

  override def modPackHint(files: Set[FileEntry]): Unit = wrap[Unit](()) { access.modPackHint(files) }
  override def latestFile(project: JsonElement): Option[FileEntry] = opt { access.latestFile(project) }
  override def allFiles(project: JsonElement): Seq[FileEntry] = wrap(Seq[FileEntry]()) { access.allFiles(project) }
  override def latestFrom(files: Set[FileEntry]): Option[FileEntry] = opt { access.latestFrom(files) }
  override def searchMods(query: String): Seq[JsonElement] = wrap(Seq[JsonElement]()) { access.searchMods(query) }
  override def dependencies(file: FileEntry): Seq[ResolvableDependency] = wrap(Seq[ResolvableDependency]()) { access.dependencies(file) }


  override def metadataChange(): Unit = access.metadataChange()
  override def destroy(): Unit = access.destroy()

  private def opt[T](action: => Option[T]): Option[T] = wrap[Option[T]](None) {
    action
  }
  
  private def wrap[T](dfl: T)(action: => T): T = try {
    action
  } catch {
    case e: Exception =>
      e.printStackTrace()
      dfl
  }
}
