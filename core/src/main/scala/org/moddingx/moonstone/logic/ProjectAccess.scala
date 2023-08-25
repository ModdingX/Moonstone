package org.moddingx.moonstone.logic

import java.net.URI
import javax.swing.JOptionPane

trait ProjectAccess {
  
  def swingFactory: SwingFactory
  def openInBrowser(uri: URI): Unit
  def writeAction[T](action: => T): T = action
}
