package org.moddingx.moonstone

import com.google.gson.{Gson, GsonBuilder}
import com.intellij.openapi.application.{ApplicationManager, ModalityState}

import java.awt.EventQueue
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

object Util {
  
  val loaders: Seq[String] = Seq(
    "forge",
    "fabric",
    "quilt",
    "modloader",
    "liteloader",
    "rift"
  )
  
  val GSON: Gson = {
    val builder = new GsonBuilder()
    builder.disableHtmlEscaping()
    builder.setLenient()
    builder.setPrettyPrinting()
    builder.create()
  }

  def getResource(path: String): InputStream = Option(Util.getClass.getResourceAsStream(path)).getOrElse(throw new IllegalStateException("Resource not found: " + path))
  
  def dispatch(action: => Unit): Unit = {
    if (EventQueue.isDispatchThread) {
      action
    } else {
      //noinspection ConvertExpressionToSAM
      SwingUtilities.invokeLater(new Runnable {
        override def run(): Unit = action
      })
    }
  }
  
  def waitForDispatch[T](action: => T): T = {
    if (EventQueue.isDispatchThread) {
      action
    } else {
      val ret = new AtomicReference[T]()
      //noinspection ConvertExpressionToSAM
      SwingUtilities.invokeAndWait(new Runnable {
        override def run(): Unit = ret.set(action)
      })
      ret.get()
    }
  }
  
  def writeAction[T](action: => T): T = {
    val ret = new AtomicReference[T]()
    //noinspection ConvertExpressionToSAM
    ApplicationManager.getApplication.invokeAndWait(new Runnable {
      override def run(): Unit = ApplicationManager.getApplication.runWriteAction(new Runnable {
        override def run(): Unit = ret.set(action)
      })
    }, ModalityState.NON_MODAL)
    ret.get()
  }
}
