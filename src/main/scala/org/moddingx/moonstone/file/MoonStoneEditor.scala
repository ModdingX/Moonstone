package org.moddingx.moonstone.file

import com.intellij.openapi.fileEditor.{FileEditor, FileEditorLocation, FileEditorState}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

import java.beans.PropertyChangeListener
import javax.swing.JComponent

class MoonStoneEditor(val project: Project, val file: VirtualFile) extends FileEditor {

  private var modified = false
  private var valid = true
  
  private lazy val component: MoonStoneComponent = new MoonStoneComponent(project, file, () => modified = true)
  
  override def getName: String = "ModList"
  override def getFile: VirtualFile = file

  override def getComponent: JComponent = component
  override def getPreferredFocusedComponent: JComponent = null

  override def isModified: Boolean = modified
  override def isValid: Boolean = valid && file.isValid

  override def setState(state: FileEditorState): Unit = ()
  override def addPropertyChangeListener(listener: PropertyChangeListener): Unit = ()
  override def removePropertyChangeListener(listener: PropertyChangeListener): Unit = ()
  override def getCurrentLocation: FileEditorLocation = null

  override def getUserData[T](key: Key[T]): T = file.getUserData(key)
  override def putUserData[T](key: Key[T], value: T): Unit = file.putUserData(key, value)

  override def dispose(): Unit = {
    valid = false
    component.dispose()
  }
}
