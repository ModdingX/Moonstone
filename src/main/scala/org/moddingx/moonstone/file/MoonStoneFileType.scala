package org.moddingx.moonstone.file

import com.intellij.openapi.fileTypes.FileType
import com.twelvemonkeys.image.BufferedImageIcon
import org.moddingx.moonstone.Util

import javax.imageio.ImageIO
import javax.swing.Icon

object MoonStoneFileType extends FileType {
  override def getName: String = "Moonstone"
  override def getDescription: String = "CurseForge mod list file"
  override def getDefaultExtension: String = "modlist.json"
  override lazy val getIcon: Icon = new BufferedImageIcon(ImageIO.read(Util.getResource("/org/moddingx/moonstone/file/icon.png")), 16, 16)
  override def isBinary: Boolean = false
}
