package io.github.noeppi_noeppi.tools.moonstone.model

import com.google.gson.{JsonArray, JsonSyntaxException}
import com.intellij.openapi.application.{ApplicationManager, TransactionGuard}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.github.noeppi_noeppi.tools.moonstone.Util

import java.io.{IOException, InputStreamReader, OutputStreamWriter}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class FileList(val project: Project, val file: VirtualFile, private val modify: () => Unit) {

  private val installed = mutable.Map[Int, FileInfo]()
  private val dependencies = mutable.Map[Int, FileInfo]()
  
  try {
    val reader = new InputStreamReader(file.getInputStream)
    val json = Util.GSON.fromJson(reader, classOf[JsonArray])
    reader.close()
    if (json != null) { // null for empty file
      for (elem <- json.asScala if elem.isJsonObject; entry = elem.getAsJsonObject) {
        val isInstalled = !entry.has("installed") || entry.get("installed").getAsBoolean
        FileInfo.fromJson(entry) match {
          case Some(info) if isInstalled =>
            installed.addOne(info.projectId -> info)
            dependencies.remove(info.projectId)
          case Some(info) if !isInstalled =>
            dependencies.addOne(info.projectId -> info)
            installed.remove(info.projectId)
          case None =>
        }
      }
    } else {
      // Write an empty file
      save()
    }
  } catch {
    case _: IOException | _: JsonSyntaxException => System.err.println("Failed to read json.")
  }
  
  def save(): Unit = {
    val json = new JsonArray()
    for (info <- installed.values.toSeq.sortBy(f => (f.projectId, f.fileId))) {
      val entry = info.toJson
      entry.addProperty("installed", true)
      json.add(entry)
    }
    for (info <- dependencies.values.toSeq.sortBy(f => (f.projectId, f.fileId))) {
      val entry = info.toJson
      entry.addProperty("installed", false)
      json.add(entry)
    }
    Util.writeAction {
      val writer = new OutputStreamWriter(file.getOutputStream(this))
      writer.write(Util.GSON.toJson(json) + "\n")
      writer.close()
    }
  }
  
  def installedFiles: Set[FileInfo] = installed.values.toSet
  def dependencyFiles: Set[FileInfo] = dependencies.values.toSet
  
  def installedMap: Map[Int, FileInfo] = installed.toMap
  def dependencyMap: Map[Int, FileInfo] = dependencies.toMap
  def allFiles: Map[Int, FileInfo] = Map.newBuilder.addAll(dependencies).addAll(installed).result()

  def hasProject(projectId: Int): Boolean = installed.contains(projectId) || dependencies.contains(projectId)
  def fileInfo(projectId: Int): Option[FileInfo] = installed.get(projectId).orElse(dependencies.get(projectId))
  
  def remove(projectId: Int): Unit = {
    val b1 = installed.remove(projectId).isDefined
    val b2 = dependencies.remove(projectId).isDefined
    if (b1 || b2) modify()
  }
  
  def removeDependency(projectId: Int): Unit = {
    if (dependencies.remove(projectId).isDefined) modify()
  }
  
  def updateOrAddDependency(file: FileInfo): Unit = {
    if (installed.contains(file.projectId)) {
      val removedDep = dependencies.remove(file.projectId).isDefined
      val hasChanges = file != installed(file.projectId)
      if (hasChanges) installed(file.projectId) = file
      if (removedDep || hasChanges) modify()
    } else if (dependencies.contains(file.projectId)) {
      if (file != dependencies(file.projectId)) {
        dependencies(file.projectId) = file
        modify()
      }
    } else {
      dependencies.put(file.projectId, file)
      modify()
    }
  }
  
  def add(file: FileInfo, isInstalled: Boolean): Unit = {
    installed.remove(file.projectId)
    dependencies.remove(file.projectId)
    if (isInstalled) {
      installed(file.projectId) = file
    } else {
      dependencies(file.projectId) = file
    }
    modify()
  }
}
