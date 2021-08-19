package io.github.noeppi_noeppi.tools.moonstone.file

import com.intellij.openapi.fileEditor.{FileEditor, FileEditorPolicy, FileEditorProvider}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class MoonStoneEditorProvider extends FileEditorProvider {

  override def accept(project: Project, file: VirtualFile): Boolean = file.getFileType == MoonStoneFileType
  override def getEditorTypeId: String = classOf[MoonStoneEditorProvider].getCanonicalName
  override def createEditor(project: Project, file: VirtualFile): FileEditor = new MoonStoneEditor(project, file)
  override def getPolicy: FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}
