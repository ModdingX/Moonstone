package io.github.noeppi_noeppi.tools.moonstone.curse

import com.google.gson.{JsonArray, JsonObject}
import com.intellij.openapi.Disposable
import io.github.noeppi_noeppi.tools.moonstone.{PackConfig, Util}
import io.github.noeppi_noeppi.tools.moonstone.util.PendingFuture

import java.awt.image.BufferedImage
import java.net.{MalformedURLException, URL}
import java.util.{Calendar, Locale}
import java.util.concurrent.{ExecutorService, Future, ScheduledExecutorService, ScheduledThreadPoolExecutor, TimeUnit}
import javax.imageio.ImageIO
import javax.xml.bind.DatatypeConverter
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

class CurseCache extends Disposable {

  private val assetDownloader: ScheduledExecutorService = new ScheduledThreadPoolExecutor(5)
  
  private val projectEntries = mutable.Map[Int, ProjectEntry]()
  private val fileEntries = mutable.Map[(Int, Int), FileEntry]()
  private val latestFiles = mutable.Map[(Int, PackConfig), Option[Int]]()
  
  private val currentListeners = mutable.Map[Int, ListBuffer[() => Unit]]()
  
  def projectName(projectId: Int): String = project(projectId).name
  def projectDescription(projectId: Int): String = project(projectId).description
  def projectAuthor(projectId: Int): String = project(projectId).author
  def projectUrl(projectId: Int): Option[URL] = project(projectId).url
  def projectImage(projectId: Int): Option[BufferedImage] = Util.query(project(projectId).thumbnail)
  
  def fileName(projectId: Int, fileId: Int): String = file(projectId, fileId).name
  def fileTargets(projectId: Int, fileId: Int): Set[String] = file(projectId, fileId).targets
  def fileDependencies(projectId: Int, fileId: Int): Set[Int] = file(projectId, fileId).dependencies
  
  def addImageResolveListener(projectId: Int, listener: () => Unit): Unit = currentListeners.synchronized {
    currentListeners.getOrElseUpdate(projectId, ListBuffer()).addOne(listener)
  }
  
  private def project(projectId: Int): ProjectEntry = projectEntries.getOrElseUpdate(projectId, {
    CurseAPI.query[JsonObject, ProjectEntry]("addon/" + projectId, json => {
      val name = json.get("name").getAsString
      val description = if (json.has("summary")) json.get("summary").getAsString else name
      val author = if (json.has("authors")) {
        json.getAsJsonArray("authors").asScala
          .flatMap(a => if (a.isJsonObject) Some(a.getAsJsonObject) else None)
          .find(a => !a.has("projectTitleId") || a.get("projectTitleId").isJsonNull)
          .flatMap(a => if (a.has("name")) Some(a.get("name").getAsString) else None)
          .getOrElse("")
      } else {
        ""
      }
      val url = if (json.has("websiteUrl")) {
        try {
          Some(new URL(json.get("websiteUrl").getAsString))
        } catch {
          case _: MalformedURLException => None
        }
      } else {
        None
      }
      val thumbnail: Future[BufferedImage] = if (json.has("attachments")) {
        json.getAsJsonArray("attachments").asScala
          .flatMap(a => if (a.isJsonObject) Some(a.getAsJsonObject) else None)
          .find(a => a.has("isDefault") && a.get("isDefault").getAsBoolean)
          .flatMap(a => if (a.has("url")) Some(a.get("url").getAsString) else None)
          .map(u => new URL(u))
          .map(url => assetDownloader.submit[BufferedImage](() => {
            val in = url.openStream()
            val img = ImageIO.read(in)
            in.close()
            currentListeners.synchronized {
              currentListeners.getOrElse(projectId, Nil).foreach(_())
              currentListeners.remove(projectId)
            }
            img
          }))
          .getOrElse(PendingFuture.instance)
      } else {
        PendingFuture.instance
      }
      ProjectEntry(name, description, author, url, thumbnail)
    }).getOrElse(EMPTY_PROJECT)
  })

  private def file(projectId: Int, fileId: Int): FileEntry = fileEntries.getOrElseUpdate((projectId, fileId), {
    CurseAPI.query[JsonObject, FileEntry]("addon/" + projectId + "/file/" + fileId, json => {
      val name = json.get("displayName").getAsString
      val targets: Set[String] = if (json.has("gameVersion")) {
        json.getAsJsonArray("gameVersion").asScala.map(d => d.getAsString).toSet
      } else {
        Set()
      }
      val dependencies: Set[Int] = if (json.has("dependencies")) {
        json.getAsJsonArray("dependencies").asScala
          .flatMap(d => if (d.isJsonObject) Some(d.getAsJsonObject) else None)
          .filter(d => d.has("type") && d.get("type").getAsInt == 3)
          .filter(d => d.has("type") && d.get("type").getAsInt == 3)
          .flatMap(d => if (d.has("addonId")) Some(d.get("addonId").getAsInt) else None )
          .toSet
      } else {
        Set()
      }
      FileEntry(name, targets, dependencies)
    }).getOrElse(EMPTY_FILE)
  })

  def projectLatest(projectId: Int, config: PackConfig): Option[Int] = latestFiles.getOrElseUpdate((projectId, config), {
    def gameVersionList(json: JsonArray): List[String] = json.asScala.map(_.getAsString.toLowerCase(Locale.ROOT)).toList
    def matchLoader(gameVersions: List[String]): Boolean = {
      if (config.loader.toLowerCase(Locale.ROOT) == "forge") {
        // Many older mods are not tagged for forge explicitly. Assume that a file
        // without loader is forge
        !gameVersions.contains("fabric") && !gameVersions.contains("rift")
      } else {
        gameVersions.contains(config.loader.toLowerCase(Locale.ROOT))
      }
    }
    
    CurseAPI.query[JsonArray, Option[Int]]("addon/" + projectId + "/files", json => {
      val matchingJsonData: List[JsonObject] = json.asScala
        .flatMap(e => if (e.isJsonObject) Some(e.getAsJsonObject) else None)
        .filter(e => e.has("id") && e.has("releaseType") && e.has("fileDate"))
        .filter(e => e.has("gameVersion") && e.get("gameVersion").isJsonArray)
        .filter(e => {
          val gameVersions = gameVersionList(e.getAsJsonArray("gameVersion"))
          matchLoader(gameVersions) && gameVersions.contains(config.minecraft)
        })
        .toSeq
        .sortBy(e => DatatypeConverter.parseDate(e.get("fileDate").getAsString))(Ordering[Calendar].reverse)
        .toList
      
      val resultNoAlpha = matchingJsonData.find(e => e.get("releaseType").getAsInt != 3).map(_.get("id").getAsInt)
      resultNoAlpha match {
        case Some(v) => Some(v)
        case None => matchingJsonData.find(e => e.get("releaseType").getAsInt == 3).map(_.get("id").getAsInt)
      }
    }).flatten
  })
  
  private val EMPTY_PROJECT = ProjectEntry("", "", "", None, PendingFuture.instance)
  private val EMPTY_FILE = FileEntry("", Set(), Set())
  
  case class ProjectEntry(name: String, description: String, author: String, url: Option[URL], thumbnail: Future[BufferedImage])
  case class FileEntry(name: String, targets: Set[String], dependencies: Set[Int])

  override def dispose(): Unit = assetDownloader.shutdownNow()
}
