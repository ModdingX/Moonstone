package io.github.noeppi_noeppi.tools.moonstone.file

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.{JBScrollPane, JBTabbedPane}
import io.github.noeppi_noeppi.tools.moonstone.curse.ModList
import io.github.noeppi_noeppi.tools.moonstone.display.ModListComponent
import io.github.noeppi_noeppi.tools.moonstone.display.part.{PackConfigSelection, TopAlignLayout}
import io.github.noeppi_noeppi.tools.moonstone.{PackConfig, Util}

import java.awt.event.{KeyAdapter, KeyEvent}
import javax.swing._

class MoonStoneComponent(val project: Project, val file: VirtualFile, private val modify: () => Unit) extends JPanel with Disposable {
  
  private val modList = new ModList(project, file, () => currentConfig(), () => rebuild(), modify)
  private lazy val detectedConfig = Util.detectConfig(project)
  
  private val installedMods: ModListComponent = new ModListComponent
  private val dependencyMods: ModListComponent = new ModListComponent
  private val searchQuery: JTextField = new JTextField()
  private val searchMods: ModListComponent = new ModListComponent
  private val selectionComponent: Option[PackConfigSelection] = detectedConfig match {
    case Some(_) => None
    case None => Some(new PackConfigSelection(() => rebuild()))
  }
  
  private def currentConfig(): PackConfig = {
    detectedConfig.orElse(selectionComponent.map(_.getCurrentConfig)).getOrElse(PackConfig("1.16.5", "Forge"))
  }
  
  private def rebuild(): Unit = {
    SwingUtilities.invokeLater(() => {
      installedMods.buildList(modList.installed())
      dependencyMods.buildList(modList.dependencies())
      searchMods.buildList(modList.search(searchQuery.getText))
      repaint()
    })
  }
  
  override def dispose(): Unit = {
    modList.dispose()
  }
  
  setLayout(new TopAlignLayout)
  
  selectionComponent match {
    case Some(c) => add(c)
    case None =>
  }
  
  private val tabbedView: JTabbedPane = new JBTabbedPane()
  tabbedView.addTab("Installed", scrollable(installedMods))
  tabbedView.addTab("Dependencies", scrollable(dependencyMods))
  
  {
    val searchPane = new JPanel()
    searchPane.setLayout(new TopAlignLayout)
    searchQuery.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit = {
        if (e.getKeyCode == KeyEvent.VK_ENTER || e.getKeyChar == '\n') {
          e.consume()
          rebuild()
        }
      }
    })
    searchPane.add(searchQuery)
    searchPane.add(scrollable(searchMods))
    tabbedView.addTab("Search", searchPane)
  }
  
  add(tabbedView)
  
  rebuild()
  
  private def scrollable(c: JComponent): JScrollPane = {
    new JBScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  }
}
