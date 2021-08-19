package io.github.noeppi_noeppi.tools.moonstone.file

import com.intellij.openapi.fileTypes.FileType
import com.twelvemonkeys.image.BufferedImageIcon
import io.github.noeppi_noeppi.tools.moonstone.Util

import javax.imageio.ImageIO
import javax.swing.Icon

object MoonStoneFileType extends FileType {
  override def getName: String = "Moonstone"
  override def getDescription: String = "CurseForge mod list file"
  override def getDefaultExtension: String = "modlist.json"
  override lazy val getIcon: Icon = new BufferedImageIcon(ImageIO.read(Util.getResource("/io/github/noeppi_noeppi/tools/moonstone/file/icon.png")), 16, 16)
  override def isBinary: Boolean = false
}
