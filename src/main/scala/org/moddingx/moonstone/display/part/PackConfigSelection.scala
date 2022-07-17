package org.moddingx.moonstone.display.part

import com.intellij.openapi.ui.Messages
import org.moddingx.moonstone.Util
import org.moddingx.moonstone.platform.{ModList, ModdingPlatform}

import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.{Dimension, FlowLayout}
import javax.swing.{JComponent, JLabel, JPanel, JTextField}

class PackConfigSelection(list: ModList) extends JPanel {

  setLayout(new FlowLayout())

  add(new JLabel("Platform:"))
  private val platform = ButtonHelper.selection[ModdingPlatform](list.platform, ModdingPlatform.platforms, _.name, (p, _) => {
    if (p != list.platform) {
      val result = Messages.showYesNoDialog(list.project, "Changing the modding platform will remove all mods from the modlist. Are you sure, you want to continue?", "Platform Change", null)
      if (result == Messages.YES) {
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
  private val loader = ButtonHelper.editableSelection(list.loader, Util.loaders, (l, _) => list.loader = l)
  add(loader)
}
