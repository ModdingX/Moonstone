package org.moddingx.moonstone.display.part

import org.moddingx.moonstone.logic.SwingFactory

import java.awt.event.ActionEvent
import javax.swing.JComponent

object ButtonHelper {
  
  def button(factory: SwingFactory, text: String, enabled: Boolean, action: ActionEvent => Unit): JComponent = DefaultButton(factory, text, enabled, action).createButton()
  def selection[T](factory: SwingFactory, value: T, options: Seq[T], text: T => String, action: (T, ActionEvent) => T): JComponent = SelectButton(factory, value, options, text, action).createButton()
  def editableSelection(factory: SwingFactory, value: String, options: Seq[String], action: (String, ActionEvent) => Unit): JComponent = EditableSelectButton(factory, value, options, action).createButton()
  
  sealed trait ButtonFactory {
    def createButton(): JComponent
  }

  case class DefaultButton(factory: SwingFactory, text: String, enabled: Boolean, action: ActionEvent => Unit) extends ButtonFactory {
    override def createButton(): JComponent = {
      val button = factory.newButton(text)
      button.setEnabled(enabled)
      button.addActionListener((e: ActionEvent) => action(e))
      button
    }
  }

  case class SelectButton[T](factory: SwingFactory, value: T, options: Seq[T], text: T => String, action: (T, ActionEvent) => T) extends ButtonFactory {
    override def createButton(): JComponent = {
      val textList = options.map(text)
      val button = factory.newComboBox(textList)
      button.setEditable(false)
      val idx = options.indexOf(value)
      button.setSelectedIndex(if (idx >= 0) idx else 0)
      
      var runListener = true
      button.addActionListener((e: ActionEvent) => {
        if (runListener && options.indices.contains(button.getSelectedIndex)) {
          val idx = button.getSelectedIndex
          if (options.indices.contains(idx)) {
            val newElem = action(options(idx), e)
            val newIdx = options.indexOf(newElem)
            if (newIdx >= 0 && newIdx != idx) {
              runListener = false
              button.setSelectedIndex(newIdx)
              runListener = true
            }
          }
        }
      })
      button
    }
  }
  
  case class EditableSelectButton(factory: SwingFactory, value: String, options: Seq[String], action: (String, ActionEvent) => Unit) extends ButtonFactory {
    override def createButton(): JComponent = {
      val button = factory.newComboBox(options)
      button.setEditable(true)
      button.setSelectedItem(value)
      button.addActionListener((e: ActionEvent) => {
        button.getSelectedItem match {
          case x: String => action(x, e)
          case _ =>
        }
      })
      button
    }
  }
}
