package io.github.noeppi_noeppi.tools.moonstone.curse

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.github.noeppi_noeppi.tools.moonstone.PackConfig
import io.github.noeppi_noeppi.tools.moonstone.display.ModUnit
import io.github.noeppi_noeppi.tools.moonstone.file.MoonStoneComponent
import io.github.noeppi_noeppi.tools.moonstone.model.{FileInfo, FileList, Side}

import java.awt.image.BufferedImage
import java.net.URL
import scala.collection.mutable

class ModList(val project: Project, val file: VirtualFile, component: MoonStoneComponent, modify: () => Unit) extends Disposable {

  private val fileList = new FileList(project, file, modify)
  private val cache = new CurseCache
  
  def installed(): List[ModUnit] = fileList.installedFiles
    .map(file => new BaseUnit(file, true))
    .toList.sortBy(u => (!u.canUpdate, u.name))
  
  def dependencies(): List[ModUnit] = fileList.dependencyFiles
    .map(file => new BaseUnit(file, false))
    .toList.sortBy(u => (!u.canUpdate, u.name))
  
  def search(query: String): List[ModUnit] = CurseAPI.searchMods(query, component.currentConfig())
    .filter(!fileList.hasProject(_))
    .map(projectId => new SearchUnit(projectId))
  
  def updateFileList(action: => Unit): Unit = {
    component.rebuild {
      action
      val installedMap = fileList.installedMap
      val allFiles = fileList.allFiles
      val (dependencies, fileIds) = collectTransitiveDependencies(installedMap.values.toSet, allFiles)
      val reversedDependencies = dependencies
        .flatMap(e => e._2.map(r => (e._1, r)))
        .map(_.swap)
        .groupBy(e => e._1)
        .map(e => (e._1, e._2.values.toSet))
      val sideLookup = createSideLookup(reversedDependencies, projectId => installedMap.get(projectId).map(_.side))
      for (currentDependency <- fileList.dependencyMap.keySet if !dependencies.contains(currentDependency)) {
        fileList.removeDependency(currentDependency)
      }
      for (projectId <- dependencies.keySet) {
        allFiles.get(projectId).orElse(fileIds.get(projectId).map(fileId => FileInfo(projectId, fileId, Side.COMMON, locked = false))) match {
          case Some(base) => fileList.updateOrAddDependency(base.withSide(sideLookup(projectId)))
          case None =>
        }
      }
      fileList.save()
    }
  }
  
  private def collectTransitiveDependencies(installed: Set[FileInfo], files: Map[Int, FileInfo]): (Map[Int, Set[Int]], Map[Int, Int]) = {
    val dependencies = mutable.Map[Int, Set[Int]]()
    val fileIds = mutable.Map[Int, Int]()
    for (FileInfo(projectId, _, _, _) <- installed) collectTransitiveDependencies(projectId, component.currentConfig(), files, dependencies, fileIds)
    (dependencies.toMap, fileIds.toMap)
  }
  
  private def collectTransitiveDependencies(projectId: Int, cfg: PackConfig, files: Map[Int, FileInfo], dependencyMap: mutable.Map[Int, Set[Int]], fileIdMap: mutable.Map[Int, Int]): Set[Int] = {
    if (!dependencyMap.contains(projectId)) {
      val set = Set.newBuilder[Int]
      val depFile = files.get(projectId).map(_.fileId).orElse(cache.projectLatest(projectId, cfg))
      if (depFile.isDefined) {
        fileIdMap(projectId) = depFile.get
        for (dep <- cache.fileDependencies(projectId, depFile.get)) {
          set.addOne(dep)
          set.addAll(collectTransitiveDependencies(dep, cfg, files, dependencyMap, fileIdMap))
        }
      }
      val result = set.result()
      dependencyMap(projectId) = result
      result
    } else {
      dependencyMap(projectId)
    }
  }
  
  private def createSideLookup(reversedDependencies: Map[Int, Set[Int]], installedFactory: Int => Option[Side]): Int => Side = {
    val map = mutable.Map[Int, Option[Side]]()
    def getSide(projectId: Int): Option[Side] = map.getOrElseUpdate(projectId, {
      val result = installedFactory(projectId)
      if (result.isDefined && result.get == Side.COMMON) {
        Some(Side.COMMON)
      } else {
        val allDepSides = reversedDependencies.getOrElse(projectId, Set()).flatMap(getSide)
        if (allDepSides.isEmpty) {
          result
        } else if (result.isDefined) {
          Some(Side.merge(result.get, Side.merge(allDepSides.toSeq: _*)))
        } else {
          Some(Side.merge(allDepSides.toSeq: _*))
        }
      }
    })
    projectId => getSide(projectId).getOrElse(Side.COMMON)
  }
  
  class BaseUnit(file: FileInfo, installed: Boolean) extends ModUnit {
    
    override val project: Project = ModList.this.project
    override def name: String = cache.projectName(file.projectId)
    override def version: Option[String] = Some(cache.fileName(file.projectId, file.fileId))
    override def description: String = cache.projectDescription(file.projectId)
    override def url(): Option[URL] = cache.projectUrl(file.projectId)
    override def side(): Side = file.side
    override def image(): Option[BufferedImage] = cache.projectImage(file.projectId)
    override def versionLockSuggestion(): Option[Int] = Some(file.fileId)

    override def addImageResolveListener(listener: () => Unit): Unit = cache.addImageResolveListener(file.projectId, listener)

    override def isSimple: Boolean = false
    override def isInstalled: Boolean = installed
    override def canUpdate: Boolean = cache.projectLatest(file.projectId, component.currentConfig()).exists(_ != file.fileId)
    override def isVersionLocked: Boolean = file.locked
    override def canSetSide: Boolean = installed
    
    override def install(): Unit = if (!installed) {
      updateFileList {
        fileList.add(file, isInstalled = true)
      }
    }
    
    override def uninstall(): Unit = if (installed) {
      updateFileList {
        fileList.remove(file.projectId)
      }
    }
    
    override def update(): Unit = {
      cache.projectLatest(file.projectId, component.currentConfig()) match {
        case Some(latest) =>
          updateFileList {
            fileList.updateOrAddDependency(file.withVersion(latest))
          }
        case None =>
      }
    }
    
    override def lock(fileId: Int): Unit = {
      if (!file.locked) {
        updateFileList {
          fileList.updateOrAddDependency(file.withVersion(fileId).withLock(true))
        }
      }
    }
    
    override def unlock(): Unit = {
      if (file.locked) {
        updateFileList {
          fileList.updateOrAddDependency(file.withLock(false))
        }
      }
    }
    
    override def setSide(side: Side): Unit = if (installed) {
      fileList.updateOrAddDependency(file.withSide(side))
      updateFileList {
        fileList.updateOrAddDependency(file.withSide(side))
      }
    }

    override def resolve(): Unit = {
      cache.projectName(file.projectId)
      cache.fileName(file.projectId, file.fileId)
      cache.projectLatest(file.projectId, component.currentConfig())
    }
  }
  
  class SearchUnit(projectId: Int) extends ModUnit {

    override val project: Project = ModList.this.project
    override def name: String = cache.projectName(projectId)
    override def version: Option[String] = None
    override def description: String = cache.projectDescription(projectId)
    override def url(): Option[URL] = cache.projectUrl(projectId)
    override def side(): Side = Side.COMMON
    override def image(): Option[BufferedImage] = cache.projectImage(projectId)
    override def versionLockSuggestion(): Option[Int] = None

    override def addImageResolveListener(listener: () => Unit): Unit = cache.addImageResolveListener(projectId, listener)

    override def isSimple: Boolean = true
    override def isInstalled: Boolean = false
    override def canUpdate: Boolean = false
    override def isVersionLocked: Boolean = false
    override def canSetSide: Boolean = false

    override def install(): Unit = {
      cache.projectLatest(projectId, component.currentConfig()) match {
        case Some(fileId) =>
          updateFileList {
            fileList.add(FileInfo(projectId, fileId, Side.COMMON, locked = false), isInstalled = true)
          }
        case None =>
      }
    }
    
    override def uninstall(): Unit = ()
    override def update(): Unit = ()
    override def lock(fileId: Int): Unit = ()
    override def unlock(): Unit = ()
    override def setSide(side: Side): Unit = ()

    override def resolve(): Unit = {
      cache.projectName(projectId)
    }
  }

  override def dispose(): Unit = cache.dispose()
}
