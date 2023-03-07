package org.moddingx.moonstone.platform.modrinth

import com.google.gson.{JsonElement, JsonPrimitive}
import org.moddingx.moonstone.model.{FileEntry, Side}
import org.moddingx.moonstone.platform.{ModList, PlatformAccess, ResolvableDependency}

import java.net.URI

class ModrinthAccess(val list: ModList) extends PlatformAccess {
  
  val cache = new ModrinthCache(list)
  
  private def asEntry(file: ModrinthVersion): FileEntry = new FileEntry(
    new JsonPrimitive(file.projectId),
    new JsonPrimitive(file.id),
    Side.COMMON, false
  )
  
  override def projectName(project: JsonElement): String = cache.project(project.getAsString).name
  override def projectDescription(project: JsonElement): String = cache.project(project.getAsString).description
  override def projectLogo(project: JsonElement): Option[URI] = cache.project(project.getAsString).icon
  override def projectSite(project: JsonElement): Option[URI] = cache.project(project.getAsString).url
  override def thirdPartyDownloads(project: JsonElement): Boolean = true
  override def defaultFileSide(file: FileEntry): Side = cache.project(file.project.getAsString).side
  override def versionName(file: FileEntry): String = cache.version(file.file.getAsString).name
  override def versionByInput(file: FileEntry, input: String): Option[FileEntry] = {
    val ver = cache.version(input)
    if (ver.projectId == file.project.getAsString) {
      Some(file.withFile(new JsonPrimitive(ver.id)))
    } else {
      None
    }
  }

  override def modPackHint(files: Set[FileEntry]): Unit = {
    cache.loadProjects(files.map(_.project.getAsString))
    cache.loadVersions(files.map(_.file.getAsString))
  }
  
  override def latestFile(project: JsonElement): Option[FileEntry] = cache.getVersions(project.getAsString).lastOption.map(asEntry)
  override def allFiles(project: JsonElement): Seq[FileEntry] = cache.getVersions(project.getAsString).map(asEntry)
  
  override def latestFrom(files: Set[FileEntry]): Option[FileEntry] = {
    cache.loadVersions(files.map(_.file.getAsString))
    files.maxByOption(entry => cache.version(entry.file.getAsString).date)
  }
  
  override def searchMods(query: String): Seq[JsonElement] = cache.search(query).map(p => new JsonPrimitive(p.id))
  
  override def dependencies(file: FileEntry): Seq[ResolvableDependency] = cache.version(file.file.getAsString).dependencies.flatMap(_.resolve(this))
  
  override def metadataChange(): Unit = cache.metadataChange()
  override def dispose(): Unit = cache.dispose()
}
