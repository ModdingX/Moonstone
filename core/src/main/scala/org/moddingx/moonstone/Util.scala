package org.moddingx.moonstone

import com.google.gson.{Gson, GsonBuilder, JsonElement, JsonObject}

import java.awt.EventQueue
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

object Util {
  
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
  
  def option(json: JsonObject, key: String): Option[JsonElement] = {
    if (json.has(key)) {
      val elem = json.get(key)
      if (elem.isJsonNull) {
        None
      } else {
        Some(elem)
      }
    } else {
      None
    }
  }
}
