package io.github.noeppi_noeppi.tools.moonstone

import com.google.gson.{Gson, GsonBuilder}
import com.intellij.openapi.project.Project
import io.github.noeppi_noeppi.tools.moonstone.util.PendingFuture
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings

import java.io.InputStream
import java.util.concurrent.Future

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
    case f if f.isDone => Some(f.get())
    case _ => None
  }
}
