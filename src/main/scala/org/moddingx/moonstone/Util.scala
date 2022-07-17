package org.moddingx.moonstone

import com.google.gson.{Gson, GsonBuilder}
import com.intellij.openapi.application.{ApplicationManager, ModalityState}
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.moddingx.moonstone.util.PendingFuture

import java.awt.EventQueue
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Future, FutureTask, TimeUnit}
import javax.swing.SwingUtilities
import scala.concurrent.{CancellationException, ExecutionException, TimeoutException}

object Util {

  val GSON: Gson = {
    val builder = new GsonBuilder()
    builder.disableHtmlEscaping()
    builder.setLenient()
    builder.setPrettyPrinting()
    builder.create()
  }

  def getResource(path: String): InputStream = Option(classOf[PackConfig].getResourceAsStream(path)).getOrElse(throw new IllegalStateException("Resource not found: " + path))

  def detectConfig(project: Project): Option[PackConfig] = {
    val settings = GradleExtensionsSettings.getInstance(project)
    if (settings == null || settings.projects.size() != 1) return None
    val gradle = settings.projects.values().iterator().next()
    if (gradle == null || gradle.extensions.size() != 1) return None
    val ext = gradle.extensions.values().iterator().next()
    if (ext.properties.containsKey("MCP_VERSION") && ext.properties.containsKey("MC_VERSION") && ext.properties.get("MC_VERSION").value != null) {
      Some(PackConfig(ext.properties.get("MC_VERSION").value, "Forge"))
    } else {
      None
    }
  }
  
  def query[T](future: Future[T]): Option[T] = future match {
    case PendingFuture => None
    case f: FutureTask[T] =>
      try {
        // FutureTask is weird. (Timeout = 0 means no time out, use 1 instead)
        Some(f.get(1, TimeUnit.MILLISECONDS))
      } catch {
        case _: TimeoutException | _: CancellationException | _: ExecutionException => None
      }
    case f if f.isDone && !f.isCancelled =>
      try {
        Some(f.get())
      } catch {
        case _: CancellationException | _: ExecutionException => None
      }
    case _ => None
  }
  
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
