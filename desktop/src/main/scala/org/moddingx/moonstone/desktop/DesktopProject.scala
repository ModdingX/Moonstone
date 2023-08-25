package org.moddingx.moonstone.desktop

import org.moddingx.moonstone.logic.{ProjectAccess, SwingFactory}

import java.awt.Desktop
import java.net.URI

object DesktopProject extends ProjectAccess {
  
  object swingFactory extends SwingFactory

  override def openInBrowser(uri: URI): Unit = if (Desktop.isDesktopSupported) {
    val desktop = Desktop.getDesktop
    if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
            desktop.browse(uri)
        } catch {
          case e: Exception => e.printStackTrace()
        }
    }
  }
}
