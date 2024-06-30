package org.moddingx.moonstone.display.part

import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JButton

class JImage(val img: () => Option[BufferedImage]) extends JButton {

  setOpaque(false)

  override def paint(g: Graphics): Unit = {
    img() match {
      case Some(image) =>
        val width = this.getWidth
        val height = this.getHeight
        val scale = (width / image.getWidth.toDouble) min (height / image.getHeight.toDouble)
        val imgWidth = Math.round(image.getWidth * scale).toInt
        val imgHeight = Math.round(image.getHeight * scale).toInt
        val paddingLeft = (width - imgWidth) / 2
        val paddingTop = (height - imgHeight) / 2
        g.drawImage(image, paddingLeft, paddingTop, imgWidth, imgHeight, this)
      case None =>
    }
  }

  override def update(g: Graphics): Unit = paint(g)
}
