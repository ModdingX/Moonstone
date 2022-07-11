package org.moddingx.moonstone.display.part

import org.moddingx.moonstone.PackConfig
import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.{Dimension, FlowLayout}
import javax.swing.{JComponent, JLabel, JPanel, JTextField}

class PackConfigSelection(rebuildAction: () => Unit) extends JPanel {

  setLayout(new FlowLayout())
  
  add(new JLabel("Minecraft Version:"))
  private val minecraft = new JTextField()
  minecraft.setText("1.16.5")
  minecraft.addKeyListener(new KeyAdapter {
    override def keyPressed(e: KeyEvent): Unit = {
      if (e.getKeyCode == KeyEvent.VK_ENTER || e.getKeyChar == '\n') {
        e.consume()
        rebuildAction()
      }
    }
  })
  add(minecraft)
  
  private val space = new JComponent {}
  space.setPreferredSize(new Dimension(30, 0))
  add(space)
  
  add(new JLabel("Modloader:"))
  private val loader = new JTextField()
  loader.setText("Forge")
  loader.addKeyListener(new KeyAdapter {
    override def keyPressed(e: KeyEvent): Unit = {
      if (e.getKeyCode == KeyEvent.VK_ENTER || e.getKeyChar == '\n') {
        e.consume()
        rebuildAction()
      }
    }
  })
  add(loader)
  
  def getCurrentConfig: PackConfig = PackConfig(minecraft.getText, loader.getText)
}
