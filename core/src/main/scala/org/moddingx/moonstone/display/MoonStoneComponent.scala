package org.moddingx.moonstone.display

import org.moddingx.moonstone.Util
import org.moddingx.moonstone.display.part.{PackConfigSelection, TopAlignLayout}
import org.moddingx.moonstone.logic.{Destroyable, FileAccess, ProjectAccess}
import org.moddingx.moonstone.model.FileList
import org.moddingx.moonstone.platform.ModList

import java.awt.event.{KeyAdapter, KeyEvent}
import java.util.concurrent.{ExecutorService, ScheduledThreadPoolExecutor}
import javax.swing._
import javax.swing.event.ChangeEvent

class MoonStoneComponent protected (val project: ProjectAccess, private val initialFileList: FileList) extends JPanel with Destroyable {
  
  private val rebuildExecutor: ExecutorService = new ScheduledThreadPoolExecutor(1)
  private val state = new StateHolder
  
  private val modList: ModList = ModList.create(project, initialFileList, this)
  
  private val installedMods: ModListComponent = new ModListComponent
  private val dependencyMods: ModListComponent = new ModListComponent
  private val searchQuery: JTextField = new JTextField()
  private val searchMods: ModListComponent = new ModListComponent
  private val selectionComponent: PackConfigSelection = new PackConfigSelection(modList)
  
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
  
  override def destroy(): Unit = {
    modList.destroy()
  }
  
  setLayout(new TopAlignLayout)
  
  add(selectionComponent)
  
  private val tabbedView: JTabbedPane = project.swingFactory.newTabbedPane()
  
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
            Util.dispatch {
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

  tabbedView.addChangeListener((_: ChangeEvent) => changeState {})
  add(tabbedView)
  
  rebuild {}

  private def scrollable(c: JComponent): JScrollPane = {
    project.swingFactory.newScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
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

object MoonStoneComponent {

  def create(project: ProjectAccess, file: FileAccess, onModified: () => Unit): Option[MoonStoneComponent] = create(project, file, onModified, (p, f) => new MoonStoneComponent(p, f))
  def create[T <: MoonStoneComponent](project: ProjectAccess, file: FileAccess, onModified: () => Unit, factory: (ProjectAccess, FileList) => T): Option[T] = {
    FileList.create(project, file, onModified).map(files => factory(project, files))
  }
}
