package org.moddingx.moonstone.file

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.{FileEditor, FileEditorLocation, FileEditorState}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

import java.beans.PropertyChangeListener
import javax.swing.{JComponent, JLabel}

class MoonStoneEditor(val project: Project, val file: VirtualFile) extends FileEditor {

  private var modified = false
  private var valid = true
  
  private lazy val component: JComponent = MoonStoneComponent.create(project, file, () => modified = true).getOrElse(new JLabel("Failed to read modlist content."))
  
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
    component match {
      case disposable: Disposable => disposable.dispose()
    }
  }
}
