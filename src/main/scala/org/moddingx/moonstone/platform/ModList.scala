package org.moddingx.moonstone.platform

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.moddingx.moonstone.file.MoonStoneComponent
import org.moddingx.moonstone.model.FileList

class ModList private (
                        val project: Project,
                        private var files: FileList,
                        private val component: MoonStoneComponent,
                      ) extends Disposable {

  private[this] var pAccess: PlatformAccess = _
  protected def access: PlatformAccess = {
    if (pAccess == null) {
      pAccess = platform.createAccess
    }
    pAccess
  }

  def loader: String = files.loader
  def loader_=(loader: String): Unit = files.loader = loader

  def mcVersion: String = files.mcVersion
  def mcVersion_=(mcVersion: String): Unit = files.mcVersion = mcVersion
  
  def platform: ModdingPlatform = files.platform
  def platform_=(newPlatform: ModdingPlatform): Unit = {
    if (files.platform != newPlatform) {
      if (pAccess != null) {
        pAccess.dispose()
        pAccess = null
      }
      files = files.deriveEmpty(newPlatform)
    }
  }
  
  override def dispose(): Unit = {
    if (pAccess != null) {
      pAccess.dispose()
    }
  }
}

object ModList {

  def create(project: Project, file: VirtualFile, component: MoonStoneComponent, onModified: () => Unit): Option[ModList] = {
    FileList.create(project, file, onModified).map(files => new ModList(project, files, component))
  }
}
