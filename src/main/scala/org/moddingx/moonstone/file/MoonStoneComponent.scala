package org.moddingx.moonstone.file

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.{JBScrollPane, JBTabbedPane}
import org.moddingx.moonstone.curse.ModList
import org.moddingx.moonstone.{PackConfig, Util}
import org.moddingx.moonstone.display.ModListComponent
import org.moddingx.moonstone.display.part.{PackConfigSelection, TopAlignLayout}

import java.awt.event.{KeyAdapter, KeyEvent}
import java.util.concurrent.{ExecutorService, ScheduledThreadPoolExecutor}
import javax.swing._
import javax.swing.event.ChangeEvent

class MoonStoneComponent(val project: Project, val file: VirtualFile, private val modify: () => Unit) extends JPanel with Disposable {
  
  private val rebuildExecutor: ExecutorService = new ScheduledThreadPoolExecutor(1)
  private val state = new StateHolder
  
  private val modList = new ModList(project, file, this, modify)
  private lazy val detectedConfig = Util.detectConfig(project)
  
  private val installedMods: ModListComponent = new ModListComponent
  private val dependencyMods: ModListComponent = new ModListComponent
  private val searchQuery: JTextField = new JTextField()
  private val searchMods: ModListComponent = new ModListComponent
  private val selectionComponent: Option[PackConfigSelection] = detectedConfig match {
    case Some(_) => None
    case None => Some(new PackConfigSelection(() => rebuild {}))
  }
  
  def currentConfig(): PackConfig = {
    detectedConfig.orElse(selectionComponent.map(_.getCurrentConfig)).getOrElse(PackConfig("1.16.5", "Forge"))
  }
  
  def rebuild(action: => Unit): Unit = {
    Util.waitForDispatch {
      installedMods.startLoad()
      dependencyMods.startLoad()
      searchMods.startLoad()
      repaint()
    }
    rebuildExecutor.submit(new Runnable {
      override def run(): Unit = {
        state.reset()
        action
        state.buildCurrent()
        repaint()
      }
    })
  }
  
  private def changeState(action: => Unit): Unit = {
    rebuildExecutor.submit(new Runnable {
      override def run(): Unit = {
        action
        state.buildCurrent()
        repaint()
      }
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
  tabbedView.addChangeListener((_: ChangeEvent) => changeState {})
  
  tabbedView.addTab("Installed", scrollable(installedMods))
  tabbedView.addTab("Dependencies", scrollable(dependencyMods))
  
  {
    val searchPane = new JPanel()
    searchPane.setLayout(new TopAlignLayout)
    searchQuery.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit = {
        if (e.getKeyCode == KeyEvent.VK_ENTER || e.getKeyChar == '\n') {
          e.consume()
          changeState {
            state.resetSearch()
            Util.waitForDispatch {
              searchMods.startLoad()
              repaint()
            }
          }
        }
      }
    })
    searchPane.add(searchQuery)
    searchPane.add(scrollable(searchMods))
    tabbedView.addTab("Search", searchPane)
  }
  
  add(tabbedView)
  
  rebuild {}
  
  private def scrollable(c: JComponent): JScrollPane = {
    new JBScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  }
  
  private class StateHolder {

    private var builtInstalled = false
    private var builtDependencies = false
    private var builtSearch = false
    
    def reset(): Unit = {
      builtInstalled = false
      builtDependencies = false
      builtSearch = false
    }
    
    def resetSearch(): Unit = {
      builtSearch = false
    }
    
    def buildCurrent(): Unit = {
      tabbedView.getSelectedIndex match {
        case 0 if !builtInstalled =>
          builtInstalled = true
          val installedModList = modList.installed()
          installedModList.foreach(_.resolve())
          Util.dispatch {
            installedMods.buildList(installedModList)
          }
        case 1 if !builtDependencies =>
          builtDependencies = true
          val dependencyModList = modList.dependencies()
          dependencyModList.foreach(_.resolve())
          Util.dispatch {
            dependencyMods.buildList(dependencyModList)
          }
        case 2 if !builtSearch =>
          builtSearch = true
          val searchModList = modList.search(searchQuery.getText)
          searchModList.foreach(_.resolve())
          Util.dispatch {
            searchMods.buildList(searchModList)
          }
        case _ =>
      }
    }
  }
}
