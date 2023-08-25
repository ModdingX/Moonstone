package org.moddingx.moonstone.util

import java.awt.image.BufferedImage
import java.io.{IOException, InputStream}
import java.net.URL
import java.util.concurrent.{ExecutorService, ScheduledThreadPoolExecutor}
import javax.imageio.ImageIO
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ImageResolver {
  
  private[this] val LOCK = new AnyRef
  
  private val queriedImages = mutable.Set[URL]()
  private val loadedImages = mutable.Map[URL, BufferedImage]()
  private val resolveListeners = mutable.Map[URL, ListBuffer[() => Unit]]()
  
  private val executor: ExecutorService = new ScheduledThreadPoolExecutor(2)
  
  def getImage(url: URL): Option[BufferedImage] = LOCK.synchronized {
    loadedImages.get(url) match {
      case Some(img) => Some(img)
      case None if !queriedImages.contains(url) =>
        queriedImages.add(url)
        executor.submit(new Runnable {
          override def run(): Unit = loadImageAsync(url)
        })
        None
      case None => None
    }
  }
  
  // Listener an be called from any thread
  def addListener(url: URL, listener: () => Unit): Unit = LOCK.synchronized {
    if (!loadedImages.contains(url)) {
      if (!queriedImages.contains(url)) {
        // Query the image
        getImage(url)
      }
      resolveListeners.getOrElseUpdate(url, ListBuffer()).addOne(listener)
    }
  }
  
  private def loadImageAsync(url: URL): Unit = {
    val in: InputStream = url.openStream()
    try {
      val img = ImageIO.read(in)
      val listeners: List[() => Unit] = LOCK.synchronized {
        loadedImages.put(url, img)
        val listeners = resolveListeners.get(url).map(_.toList).getOrElse(Nil)
        resolveListeners.remove(url)
        listeners
      }
      for (listener <- listeners) {
        listener.apply()
      }
    } finally {
      try {
        in.close()
      } catch {
        case _: IOException =>
      }
    }
  }
}
