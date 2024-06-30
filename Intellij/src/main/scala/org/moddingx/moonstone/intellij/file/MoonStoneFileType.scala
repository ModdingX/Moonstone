package org.moddingx.moonstone.intellij.file

import com.intellij.openapi.fileTypes.FileType
import org.moddingx.moonstone.Util

import java.awt.{Component, Graphics, Image}
import javax.imageio.ImageIO
import javax.swing.{Icon, ImageIcon}

object MoonStoneFileType extends FileType {
  override def getName: String = "Moonstone"
  override def getDescription: String = "Minecraft mod list file"
  override def getDefaultExtension: String = "modlist.json"
  override lazy val getIcon: Icon = new ScaledIcon(ImageIO.read(Util.getResource("/org/moddingx/moonstone/intellij/file/icon.png")), 16, 16)
  override def isBinary: Boolean = false
  
  private class ScaledIcon(img: Image, imgWidth: Int, imgHeight: Int) extends ImageIcon(img) {
    override def getIconWidth: Int = imgWidth
    override def getIconHeight: Int = imgHeight
    override def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
      val observer = Option(getImageObserver).getOrElse(c)
      g.drawImage(this.getImage, x, y, imgWidth, imgHeight, observer)
    }
  }
}
