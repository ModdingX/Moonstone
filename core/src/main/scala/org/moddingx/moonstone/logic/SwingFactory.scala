package org.moddingx.moonstone.logic

import java.awt.Component
import javax.swing.{JButton, JComboBox, JOptionPane, JScrollPane, JTabbedPane}

trait SwingFactory {
  
  def newButton(text: String): JButton = new JButton(text)
  def newComboBox(options: Seq[String]): JComboBox[String] = new JComboBox(options.toArray)
  def newTabbedPane(): JTabbedPane = new JTabbedPane()
  def newScrollPane(view: Component, verticalBar: Int, horizontalBar: Int): JScrollPane = {
    val pane = new JScrollPane(view, verticalBar, horizontalBar)
    if (pane.getVerticalScrollBar != null) pane.getVerticalScrollBar.setUnitIncrement(8)
    pane
  }

  def showYesNo(title: String, message: String): Boolean = {
    val result = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
    result == JOptionPane.YES_OPTION
  }

  def showInput(title: String, message: String, initial: Option[String]): String = {
    JOptionPane.showInputDialog(null, message, title, JOptionPane.QUESTION_MESSAGE, null, null, initial.orNull).asInstanceOf[String]
  }
}
