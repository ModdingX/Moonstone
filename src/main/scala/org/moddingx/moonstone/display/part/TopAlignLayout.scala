package org.moddingx.moonstone.display.part

import java.awt.{Component, Container, Dimension, LayoutManager}

class TopAlignLayout extends LayoutManager {
  
  override def addLayoutComponent(name: String, comp: Component): Unit = ()
  override def removeLayoutComponent(comp: Component): Unit = ()
  
  override def preferredLayoutSize(parent: Container): Dimension = {
    if (parent.getComponents.isEmpty) {
      new Dimension(0, 0)
    } else {
      parent.getComponent(0).getPreferredSize
    }
  }

  override def minimumLayoutSize(parent: Container): Dimension = {
    if (parent.getComponents.isEmpty) {
      new Dimension(0, 0)
    } else {
      val width = parent.getComponents.map(_.getPreferredSize.width).max
      val height = parent.getComponents.map(_.getPreferredSize.height).sum
      new Dimension(width, height)
    }
  }

  override def layoutContainer(parent: Container): Unit = {
    if (parent.getComponents.nonEmpty) {
      var currentY = 0
      for (component <- parent.getComponents.init) {
        val height = component.getPreferredSize.height
        component.setBounds(0, currentY, parent.getWidth, height)
        currentY += height
      }
      parent.getComponents.last.setBounds(0, currentY, parent.getWidth, 0 max (parent.getHeight - currentY))
    }
  }
}
