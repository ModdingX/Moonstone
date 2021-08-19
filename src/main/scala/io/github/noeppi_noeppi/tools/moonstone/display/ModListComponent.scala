package io.github.noeppi_noeppi.tools.moonstone.display

import java.awt.{Component, Container, Dimension, FlowLayout, LayoutManager}
import javax.swing.{JLabel, JPanel}
import scala.collection.mutable.ListBuffer

class ModListComponent() extends JPanel {
  
  def buildList(mods: List[ModUnit]): Unit = {
    removeAll()
    if (mods.isEmpty) {
      setLayout(new FlowLayout())
      add(new JLabel("No mods found"))
    } else {
      setLayout(new Layout)
      mods.foreach(mod => add(new ModComponent(mod)))
    }
  }
  
  class Layout extends LayoutManager {
    
    private val components = ListBuffer[Component]()
    
    private def getMods(parent: Container): List[ModComponent] = parent.getComponents.toList.flatMap {
      case c: ModComponent => Some(c)
      case _ => None
    }
    
    override def addLayoutComponent(name: String, comp: Component): Unit = components.addOne(comp)
    override def removeLayoutComponent(comp: Component): Unit = components.filterInPlace(_ != comp)
    override def minimumLayoutSize(parent: Container): Dimension = preferredLayoutSize(parent)
    
    override def preferredLayoutSize(parent: Container): Dimension = {
      val mods = getMods(parent)
      if (mods.isEmpty) return new Dimension(0, 0)
      val initSize = mods.map(_.suggestedInitSize).max
      val baseWidth = mods.map(_.calculatePreferredSize(initSize)).map(_.getWidth.toInt).max
      val width = if (parent.getSize.getWidth > 0) parent.getSize.getWidth.toInt min baseWidth else baseWidth
      val height = mods.map(_.calculatePreferredSize(initSize)).map(_.height + 20).sum
      new Dimension(width + 20, height)
    }
    
    override def layoutContainer(parent: Container): Unit = {
      val mods = getMods(parent)
      if (mods.nonEmpty) {
        val initSize = mods.map(_.suggestedInitSize).max
        val baseWidth = mods.map(_.calculatePreferredSize(initSize)).map(_.getWidth.toInt).max
        val width = if (parent.getSize.getWidth > 0) (0 max (parent.getSize.getWidth.toInt - 20)) min baseWidth else baseWidth
        var currentY = 0
        for (mod <- mods) {
          val height = mod.calculatePreferredSize(initSize).height
          mod.initSize(initSize)
          mod.setBounds(0, currentY, width, height)
          mod.doLayout()
          currentY += height
          currentY += 20
        }
      }
    }
  }
}
