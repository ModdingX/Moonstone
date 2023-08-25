package org.moddingx.moonstone.platform

import com.google.gson.{JsonElement, JsonParseException}
import org.moddingx.moonstone.Util
import org.moddingx.moonstone.display.{ModUnit, MoonStoneComponent}
import org.moddingx.moonstone.logic.{Destroyable, FileAccess, ProjectAccess}
import org.moddingx.moonstone.model.{FileEntry, FileList, Side}
import org.moddingx.moonstone.util.ImageResolver

import java.awt.image.BufferedImage
import java.net.{MalformedURLException, URI, URL}
import java.util.Locale

class ModList private (
                        val project: ProjectAccess,
                        private var files: FileList,
                        private val component: MoonStoneComponent,
                      ) extends Destroyable {

  private val imageResolver = new ImageResolver()
  
  @volatile
  private[this] var pAccess: PlatformAccess = _
  def access: PlatformAccess = {
    if (pAccess == null) {
      pAccess = new WrappedAccess(platform.createAccess(this))
      pAccess.modPackHint(files.allFiles)
    }
    pAccess
  }

  def loader: String = files.loader
  def loader_=(loader: String): Unit = {
    files.loader = loader
    if (pAccess != null) pAccess.metadataChange()
    updateFileList {}
  }

  def mcVersion: String = files.mcVersion
  def mcVersion_=(mcVersion: String): Unit = {
    files.mcVersion = mcVersion
    if (pAccess != null) pAccess.metadataChange()
    updateFileList {}
  }
  
  def platform: ModdingPlatform = files.platform
  def platform_=(newPlatform: ModdingPlatform): Unit = {
    if (files.platform != newPlatform) {
      if (pAccess != null) {
        pAccess.destroy()
        pAccess = null
      }
      files = files.deriveEmpty(newPlatform)
      updateFileList {}
    }
  }
  
  override def destroy(): Unit = {
    if (pAccess != null) {
      pAccess.destroy()
    }
    files.save()
  }

  def installed(): Seq[ModUnit] = files.installedFiles
    .map(file => new BaseUnit(file, true))
    .toSeq.sortBy(u => (!u.canUpdate, u.name))

  def dependencies(): Seq[ModUnit] = files.dependencyFiles
    .map(file => new BaseUnit(file, false))
    .toSeq.sortBy(u => (!u.canUpdate, u.name.toLowerCase(Locale.ROOT)))

  def search(query: String): Seq[ModUnit] = access.searchMods(query)
    .filter(!files.hasProject(_))
    .map(projectId => new SearchUnit(projectId))

  def updateFileList(action: => Unit): Unit = {
    component.rebuild {
      action
      val newDeps: Set[FileEntry] = DependencyLookup.lookupDependencies(this, files.installedFiles, files.dependencyFiles)
      files.setDependencies(newDeps)
      files.save()
    }
  }

  abstract class ProjectUnit(projectId: JsonElement, val localAccess: PlatformAccess) extends ModUnit {
    
    protected lazy val latestFile: Option[FileEntry] = {
      localAccess.latestFile(projectId) match {
        case Some(file) if projectId == file.project => Some(file.withSide(Side.COMMON).withLock(false))
        case _ => None
      }
    }
    
    override val project: ProjectAccess = ModList.this.project
    override lazy val name: String = localAccess.projectName(projectId)
    override lazy val description: String = localAccess.projectDescription(projectId)
    private lazy val imageURL: Option[URL] = localAccess.projectLogo(projectId).flatMap(uri => {
      try {
        Some(uri.toURL)
      } catch {
        case _: MalformedURLException => None
      }
    })
    override def image: Option[BufferedImage] = imageURL.flatMap(url => imageResolver.getImage(url)) // No lazy val, value changes when image loads
    override lazy val url: Option[URI] = localAccess.projectSite(projectId)
    
    override def addImageResolveListener(listener: () => Unit): Unit = imageURL.foreach(url => imageResolver.addListener(url, listener))
    override def allowsThirdPartyDownloads: Boolean = localAccess.thirdPartyDownloads(projectId)
    
    override def resolve(): Unit = {
      name
      version
      description
      imageURL
      url
      
      // Don't resolve latestFile here, as for search units it's useless
      // Instead call canUpdate which forces latest to be resolved if needed
      canUpdate
    }
  }
  
  class BaseUnit(file: FileEntry, installed: Boolean) extends ProjectUnit(file.project, access) {
    
    override lazy val version: Option[String] = Some(localAccess.versionName(file))
    override def side: Side = file.side
    override def versionLockSuggestion: Option[String] = Some(file.file.toString)
    
    override def isSimple: Boolean = false
    override def isInstalled: Boolean = installed
    override def canUpdate: Boolean = latestFile match {
      case Some(latest) => file.file != latest.file
      case None => false
    }
    override def isVersionLocked: Boolean = file.locked
    override def canSetSide: Boolean = installed

    override def install(): Unit = updateFileList {
      files.add(file, isInstalled = true)
    }

    override def uninstall(): Unit = updateFileList {
      files.removeProject(file)
    }

    override def update(): Unit = latestFile match {
      case Some(latest) if file.file != latest.file => updateFileList {
        files.updateOrAddDependency(latest)
      }
      case _ => 
    }

    override def lock(input: String): Unit = if (!file.locked) {
      val fileId: Option[JsonElement] = try {
        Some(Util.GSON.fromJson(input, classOf[JsonElement]))
      } catch {
        case _: JsonParseException => None
      }
      
      fileId match {
        case Some(lockVersion) => platform.validateEntry(file.withFile(lockVersion)) match {
          case Some(lockFile) => updateFileList {
            files.updateOrAddDependency(lockFile.withLock(true))
          }
          case None =>
        }
        case None => 
      }
    }

    override def unlock(): Unit = if (file.locked) {
      updateFileList {
        files.updateOrAddDependency(file.withLock(false))
      }
    }

    override def setSide(side: Side): Unit = if (installed) {
      updateFileList {
        files.updateOrAddDependency(file.withSide(side))
      }
    }
  }
  
  class SearchUnit(projectId: JsonElement) extends ProjectUnit(projectId, access) {
    
    override def version: Option[String] = None
    override def side: Side = Side.COMMON
    override def versionLockSuggestion: Option[String] = None
    
    override def isSimple: Boolean = true
    override def isInstalled: Boolean = false
    override def canUpdate: Boolean = false
    override def isVersionLocked: Boolean = false
    override def canSetSide: Boolean = false

    override def install(): Unit = latestFile match {
      case Some(latest) => updateFileList {
        files.add(latest.withSide(localAccess.defaultFileSide(latest)), isInstalled = true)
      }
      case None =>
    }
    
    override def uninstall(): Unit = ()
    override def update(): Unit = ()
    override def lock(input: String): Unit = ()
    override def unlock(): Unit = ()
    override def setSide(side: Side): Unit = ()
  }
}

object ModList {

  def create(project: ProjectAccess, file: FileAccess, component: MoonStoneComponent, onModified: () => Unit): Option[ModList] = {
    FileList.create(project, file, onModified).map(files => new ModList(project, files, component))
  }
  
  def create(project: ProjectAccess, fileList: FileList, component: MoonStoneComponent): ModList = new ModList(project, fileList, component)
}
