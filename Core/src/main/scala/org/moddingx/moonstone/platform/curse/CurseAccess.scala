package org.moddingx.moonstone.platform.curse

import com.google.gson.{JsonElement, JsonPrimitive}
import org.moddingx.cursewrapper.api.CurseWrapper
import org.moddingx.cursewrapper.api.request.FileFilter
import org.moddingx.cursewrapper.api.response.{FileEnvironment, FileInfo, ModLoader, ProjectInfo, RelationType}
import org.moddingx.moonstone.model.{FileEntry, Side}
import org.moddingx.moonstone.platform.{ModList, PlatformAccess, ProjectDependency, ResolvableDependency}

import java.net.URI
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class CurseAccess(val list: ModList) extends PlatformAccess {

  private val api = new CurseWrapper(URI.create("https://curse.melanx.de"))
  private val projects = mutable.Map[Int, ProjectInfo]()
  private val files = mutable.Map[(Int, Int), FileInfo]()
  private val latestFiles = mutable.Map[Int, FileInfo]()
  private val allFiles = mutable.Map[Int, Seq[FileInfo]]()
  private val searchResults = mutable.Map[String, Seq[Int]]()
  
  private def getProject(id: JsonElement): ProjectInfo = {
    projects.getOrElseUpdate(id.getAsInt, api.getProject(id.getAsInt))
  }

  private def getFile(file: FileEntry): FileInfo = {
    files.getOrElseUpdate((file.project.getAsInt, file.file.getAsInt), api.getFile(file.project.getAsInt, file.file.getAsInt))
  }
  
  private def asEntry(file: FileInfo): FileEntry = new FileEntry(
    new JsonPrimitive(file.projectId()),
    new JsonPrimitive(file.fileId()),
    Side.COMMON, false
  )
  
  private def filter: FileFilter = {
    val loaders: Set[ModLoader] = list.supportedLoaders.map {
      case "liteloader" => "lite_loader"
      case loader => loader
    }.map(ModLoader.get).removedAll(Set(ModLoader.UNKNOWN))
    FileFilter.create(list.mcVersion, loaders.toSeq: _*)
  }
  
  override def projectName(project: JsonElement): String = getProject(project).name()
  override def projectDescription(project: JsonElement): String = getProject(project).summary()
  override def projectLogo(project: JsonElement): Option[URI] = Some(getProject(project).thumbnail())
  override def projectSite(project: JsonElement): Option[URI] = Some(getProject(project).website())
  override def defaultFileSide(file: FileEntry): Side = getFile(file).environment() match {
    case FileEnvironment.CLIENT => Side.CLIENT
    case FileEnvironment.SERVER => Side.SERVER
    case _ => Side.COMMON
  }
  override def thirdPartyDownloads(project: JsonElement): Boolean = getProject(project).distribution()
  override def versionName(file: FileEntry): String = getFile(file).name()
  override def versionByInput(file: FileEntry, input: String): Option[FileEntry] = input.toIntOption.map(id => file.withFile(new JsonPrimitive(id)))

  override def modPackHint(files: Set[FileEntry]): Unit = ()

  override def latestFile(project: JsonElement): Option[FileEntry] = {
    val result = Option(latestFiles.getOrElseUpdate(project.getAsInt, api.getLatestFile(project.getAsInt, filter)))
    for (res <- result) files.put((res.projectId(), res.fileId()), res)
    result.map(asEntry)
  }
  
  override def allFiles(project: JsonElement): Seq[FileEntry] = {
    val results = allFiles.getOrElseUpdate(project.getAsInt, api.getFiles(project.getAsInt, filter).asScala.toSeq)
    for (res <- results) files.put((res.projectId(), res.fileId()), res)
    results.map(asEntry)
  }
  
  override def latestFrom(files: Set[FileEntry]): Option[FileEntry] = files.maxByOption(_.file.getAsInt)
  
  override def searchMods(query: String): Seq[JsonElement] = searchResults.getOrElseUpdate(query, {
    val results = api.searchMods(query, filter).asScala.toSeq
    for (res <- results) projects.put(res.projectId(), res)
    results.map(info => info.projectId())
  }).map(id => new JsonPrimitive(id))

  override def dependencies(file: FileEntry): Seq[ResolvableDependency] = getFile(file).dependencies().asScala.toSeq
    .filter(dep => dep.`type`() == RelationType.REQUIRED)
    .map(dep => ProjectDependency(new JsonPrimitive(dep.projectId()), list))

  override def metadataChange(): Unit = {
    latestFiles.clear()
    allFiles.clear()
    searchResults.clear()
  }

  override def destroy(): Unit = {
    projects.clear()
    files.clear()
    latestFiles.clear()
    allFiles.clear()
    searchResults.clear()
  }
}
