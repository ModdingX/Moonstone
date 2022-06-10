package io.github.noeppi_noeppi.tools.moonstone.curse

import com.intellij.openapi.Disposable
import org.moddingx.cursewrapper.api.CurseWrapper
import org.moddingx.cursewrapper.api.request.FileFilter
import org.moddingx.cursewrapper.api.response.{Dependency, FileInfo, ModLoader, ProjectInfo, RelationType, ReleaseType}
import io.github.noeppi_noeppi.tools.moonstone.util.PendingFuture
import io.github.noeppi_noeppi.tools.moonstone.{PackConfig, Util}

import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URI
import java.time.Instant
import java.util.concurrent.{Future, ScheduledExecutorService, ScheduledThreadPoolExecutor}
import javax.imageio.ImageIO
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

class CurseCache extends Disposable {

  private val api = new CurseWrapper(URI.create("https://curse.melanx.de/"))
  private val assetDownloader: ScheduledExecutorService = new ScheduledThreadPoolExecutor(5)
  
  private val projectEntries = mutable.Map[Int, Option[ProjectInfo]]()
  private val fileEntries = mutable.Map[(Int, Int), Option[FileInfo]]()
  private val latestFiles = mutable.Map[(Int, PackConfig), Option[Int]]()
  private val projectThumbnails = mutable.Map[Int, BufferedImage]()
  private val projectThumbnailFutures = mutable.Map[Int, Future[BufferedImage]]()
  
  private val currentListeners = mutable.Map[Int, ListBuffer[() => Unit]]()
  
  def projectName(projectId: Int): String = project(projectId).map(_.name()).getOrElse("")
  def projectDescription(projectId: Int): String = project(projectId).map(_.summary()).getOrElse("")
  def projectAuthor(projectId: Int): String = project(projectId).map(_.owner()).getOrElse("")
  def projectUrl(projectId: Int): Option[URI] = project(projectId).map(_.website())
  def projectImage(projectId: Int): Option[BufferedImage] = projectThumbnails.synchronized {
    projectThumbnails.get(projectId) match {
      case Some(image) => Some(image)
      case None =>
        Util.query(projectImageFuture(projectId)) match {
          case Some(result) =>
            projectThumbnails.put(projectId, result)
            Some(result)
          case None => None
        }
    }
  }
  def projectDistribution(projectId: Int): Boolean = project(projectId).map(_.distribution()).get
  
  private def projectImageFuture(projectId: Int): Future[BufferedImage] = projectThumbnailFutures.getOrElseUpdate(projectId, {
    project(projectId) match {
      case None => PendingFuture.instance
      case Some(project) =>
        assetDownloader.submit[BufferedImage](() => {
          val in = project.thumbnail().toURL.openStream()
          val img = ImageIO.read(in)
          in.close()
          projectThumbnails.synchronized {
            projectThumbnails.put(projectId, img)
          }
          currentListeners.synchronized {
            currentListeners.getOrElse(projectId, Nil).foreach(_())
            currentListeners.remove(projectId)
          }
          img
        })
    }
  })
  
  def fileName(projectId: Int, fileId: Int): String = file(projectId, fileId).map(_.name()).getOrElse("")
  def fileDependencies(projectId: Int, fileId: Int): Set[Int] = file(projectId, fileId).map(dep => dep.dependencies().asScala.filter(_.`type` == RelationType.REQUIRED).map((dep: Dependency) => dep.projectId()).toSet).getOrElse(Set())
  
  def addImageResolveListener(projectId: Int, listener: () => Unit): Unit = currentListeners.synchronized {
    currentListeners.getOrElseUpdate(projectId, ListBuffer()).addOne(listener)
  }
  
  private def project(projectId: Int): Option[ProjectInfo] = projectEntries.getOrElseUpdate(projectId, {
    try {
      Some(api.getProject(projectId))
    } catch {
      case _: IOException => None
    }
  })

  private def file(projectId: Int, fileId: Int): Option[FileInfo] = fileEntries.getOrElseUpdate((projectId, fileId), {
    try {
      Some(api.getFile(projectId, fileId))
    } catch {
      case _: IOException => None
    }
  })

  def projectLatest(projectId: Int, config: PackConfig): Option[Int] = latestFiles.getOrElseUpdate((projectId, config), {
    val items: Seq[FileInfo] = api.getFiles(projectId, fileFilter(config)).asScala.toSeq.filter(file => file.releaseType() != ReleaseType.ALPHA)
    items.sortBy[Instant](file => file.fileDate())(Ordering[Instant].reverse).headOption.map(_.fileId())
  })

  def searchMods(term: String, config: PackConfig): Seq[Int] = {
    val results = api.searchMods(term, fileFilter(config)).asScala.toSeq
    results.foreach(project => projectEntries.put(project.projectId(), Some(project)))
    results.map(_.projectId())
  }

  private def fileFilter(config: PackConfig): FileFilter = FileFilter.create(ModLoader.get(config.loader), config.minecraft)
  
  override def dispose(): Unit = assetDownloader.shutdownNow()
}
