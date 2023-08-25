package org.moddingx.moonstone.display.part

import org.moddingx.moonstone.LoaderConstants
import org.moddingx.moonstone.platform.{ModList, ModdingPlatform}

import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.{Dimension, FlowLayout}
import javax.swing.{JComponent, JLabel, JPanel, JTextField}

class PackConfigSelection(list: ModList) extends JPanel {

  setLayout(new FlowLayout())

  add(new JLabel("Platform:"))
  private val platform = ButtonHelper.selection[ModdingPlatform](list.project.swingFactory, list.platform, ModdingPlatform.platforms, _.name, (p, _) => {
    if (p != list.platform) {
      if (list.project.swingFactory.showYesNo("Platform Change", "Changing the modding platform will remove all mods from the modlist. Are you sure, you want to continue?")) {
        list.platform = p
        p
      } else {
        list.platform
      }
    } else {
      list.platform
    }
  })
  add(platform)
  
  private val space1 = new JComponent {}
  space1.setPreferredSize(new Dimension(30, 0))
  add(space1)
  
  add(new JLabel("Minecraft Version:"))
  private val minecraft = new JTextField()
  minecraft.setText(list.mcVersion)
  minecraft.setPreferredSize(new Dimension(100, minecraft.getPreferredSize.height))
  minecraft.addKeyListener(new KeyAdapter {
    override def keyPressed(e: KeyEvent): Unit = {
      if (e.getKeyCode == KeyEvent.VK_ENTER || e.getKeyChar == '\n') {
        e.consume()
        list.mcVersion = minecraft.getText.strip()
      }
    }
  })
  add(minecraft)
  
  private val space2 = new JComponent {}
  space2.setPreferredSize(new Dimension(30, 0))
  add(space2)
  
  add(new JLabel("Loader:"))
  private val loader = ButtonHelper.editableSelection(list.project.swingFactory, list.loader, LoaderConstants.SuggestedLoaders, (l, _) => list.loader = l)
  add(loader)
}
