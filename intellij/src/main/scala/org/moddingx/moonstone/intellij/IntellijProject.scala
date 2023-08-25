package org.moddingx.moonstone.intellij

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.{ApplicationManager, ModalityState}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{ComboBox, Messages}
import com.intellij.ui.components.{JBScrollPane, JBTabbedPane}
import org.moddingx.moonstone.logic.{ProjectAccess, SwingFactory}

import java.awt.Component
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import javax.swing.{JComboBox, JScrollPane, JTabbedPane}
import scala.reflect.ClassTag

class IntellijProject(val project: Project) extends ProjectAccess {
  
  override val swingFactory: SwingFactory = new Swing(project)
  override def openInBrowser(uri: URI): Unit = BrowserUtil.browse(uri)
  
  override def writeAction[T](action: => T): T = {
    val ret = new AtomicReference[T]()
    //noinspection ConvertExpressionToSAM
    ApplicationManager.getApplication.invokeAndWait(new Runnable {
      override def run(): Unit = ApplicationManager.getApplication.runWriteAction(new Runnable {
        override def run(): Unit = ret.set(action)
      })
    }, ModalityState.NON_MODAL)
    ret.get()
  }

  private class Swing(val project: Project) extends SwingFactory {
    override def newComboBox(options: Seq[String]): JComboBox[String] = new ComboBox(options.toArray)
    override def newTabbedPane(): JTabbedPane = new JBTabbedPane()
    override def newScrollPane(view: Component, verticalBar: Int, horizontalBar: Int): JScrollPane = new JBScrollPane(view, verticalBar, horizontalBar)
    
    override def showYesNo(title: String, message: String): Boolean = {
      val result = Messages.showYesNoDialog(project, message, title, null)
      result == Messages.YES
    }
    
    override def showInput(title: String, message: String, initial: Option[String]): String = {
      Messages.showInputDialog(project, message, title, Messages.getQuestionIcon, initial.orNull, null)
    }
  }
}
