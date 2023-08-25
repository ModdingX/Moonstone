package org.moddingx.moonstone.model

import com.google.gson.{JsonElement, JsonSyntaxException}
import org.moddingx.moonstone.logic.{FileAccess, ProjectAccess}
import org.moddingx.moonstone.platform.ModdingPlatform

import java.io.{IOException, InputStreamReader, OutputStreamWriter}
import java.util.Locale
import scala.collection.mutable

class FileList private (
                         val project: ProjectAccess,
                         val file: FileAccess,
                         private val onModified: () => Unit,
                         val platform: ModdingPlatform,
                         private[this] var pLoader: String,
                         private[this] var pMcVersion: String,
                         pInstalled: Set[FileEntry],
                         pDependencies: Set[FileEntry]
                       ) {
  
  private val installedMap: mutable.Map[JsonElement, FileEntry] = pInstalled.groupBy(_.project).view.mapValues(_.head).to(mutable.Map)
  private val dependencyMap: mutable.Map[JsonElement, FileEntry] = pDependencies.groupBy(_.project).view.mapValues(_.head).to(mutable.Map)
  
  def save(): Unit = {
    project.writeAction {
      val writer = new OutputStreamWriter(file.openForWriting(this))
      FileListIO.save(writer, FileListIO.Data(platform, loader, mcVersion, installedMap.values.toSet, dependencyMap.values.toSet))
      writer.close()
    }
  }
  
  def installedFiles: Set[FileEntry] = installedMap.values.toSet
  def dependencyFiles: Set[FileEntry] = dependencyMap.values.toSet
  def allFiles: Set[FileEntry] = installedMap.values.toSet | dependencyMap.values.toSet

  def hasProject(file: FileEntry): Boolean = hasProject(file.project)
  def hasProject(project: JsonElement): Boolean = installedMap.contains(project) || dependencyMap.contains(project)
  def fileInfo(project: JsonElement): Option[FileEntry] = installedMap.get(project).orElse(dependencyMap.get(project))
  
  def removeProject(file: FileEntry): Unit = removeProject(file.project)
  def removeProject(project: JsonElement): Unit = {
    val b1 = installedMap.remove(project).isDefined
    val b2 = dependencyMap.remove(project).isDefined
    if (b1 || b2) onModified()
  }
  
  def removeDependencyProject(file: FileEntry): Unit = removeDependencyProject(file.project)
  def removeDependencyProject(project: JsonElement): Unit = {
    if (dependencyMap.remove(project).isDefined) onModified()
  }
  
  def updateOrAddDependency(file: FileEntry): Unit = {
    if (installedMap.contains(file.project)) {
      val removedDep = dependencyMap.remove(file.project).isDefined
      val hasChanges = file != installedMap(file.project)
      if (hasChanges) installedMap(file.project) = file
      if (removedDep || hasChanges) onModified()
    } else if (dependencyMap.contains(file.project)) {
      if (file != dependencyMap(file.project)) {
        dependencyMap(file.project) = file
        onModified()
      }
    } else {
      dependencyMap.put(file.project, file)
      onModified()
    }
  }
  
  def add(file: FileEntry, isInstalled: Boolean): Unit = {
    installedMap.remove(file.project)
    dependencyMap.remove(file.project)
    if (isInstalled) {
      installedMap(file.project) = file
    } else {
      dependencyMap(file.project) = file
    }
    onModified()
  }

  def setDependencies(files: Set[FileEntry]): Unit = {
    dependencyMap.clear()
    for (file <- files) {
      dependencyMap(file.project) = file
    }
    onModified()
  }

  def loader: String = pLoader
  def loader_=(loader: String): Unit = {
    val didChange = pLoader != loader.toLowerCase(Locale.ROOT)
    if (didChange) {
      pLoader = loader.toLowerCase(Locale.ROOT)
      onModified()
    }
  }
  
  def mcVersion: String = pMcVersion
  def mcVersion_=(mcVersion: String): Unit = {
    val didChange = pMcVersion != mcVersion.toLowerCase(Locale.ROOT)
    if (didChange) {
      pMcVersion = mcVersion.toLowerCase(Locale.ROOT)
      onModified()
    }
  }
  
  def deriveEmpty(newPlatform: ModdingPlatform): FileList = {
    val derived = new FileList(project, file, onModified, newPlatform, loader, mcVersion, Set(), Set())
    derived.onModified()
    derived
  }
}

object FileList {
  
  def create(project: ProjectAccess, file: FileAccess, onModified: () => Unit): Option[FileList] = {
    try {
      val reader = new InputStreamReader(file.openForReading(this))
      val FileListIO.ReadResult(data, needsUpdate) = FileListIO.load(reader)
      
      val list = new FileList(project, file, onModified, data.platform, data.loader, data.mcVersion, data.installed, data.dependencies)
      if (needsUpdate) {
        list.save()
      }
      
      Some(list)
    } catch {
      case e @ (_: IOException | _: JsonSyntaxException) =>
        System.err.println("Failed to read json.")
        e.printStackTrace()
        None
    }
  }
}
