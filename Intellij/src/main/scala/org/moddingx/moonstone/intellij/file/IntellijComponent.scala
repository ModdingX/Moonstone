package org.moddingx.moonstone.intellij.file

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.moddingx.moonstone.display.MoonStoneComponent
import org.moddingx.moonstone.intellij.{IntellijFile, IntellijProject}
import org.moddingx.moonstone.logic.{FileAccess, ProjectAccess}
import org.moddingx.moonstone.model.FileList

class IntellijComponent private (project: ProjectAccess, private val initialFileList: FileList) extends MoonStoneComponent(project, initialFileList) with Disposable {
  override def dispose(): Unit = destroy()
}

object IntellijComponent {
  def create(project: Project, file: VirtualFile, onModified: () => Unit): Option[IntellijComponent] = {
    val projectAccess = new IntellijProject(project)
    val fileAccess = new IntellijFile(file)
    MoonStoneComponent.create(projectAccess, fileAccess, onModified, (p, f) => new IntellijComponent(p, f))
  }
}
