package org.moddingx.moonstone.desktop

import joptsimple.util.PathConverter
import joptsimple.{OptionParser, OptionSet, OptionSpec}
import org.moddingx.moonstone.display.MoonStoneComponent

import java.awt.Dimension
import java.awt.event.{WindowAdapter, WindowEvent}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import javax.swing.{JFrame, JOptionPane, UIManager, WindowConstants}

object Main extends App {

  private val options: OptionParser = new OptionParser(false)
  private val specLight: OptionSpec[Void] = options.accepts("light", "Use light theme")
  private val specHelp: OptionSpec[Void] = options.accepts("help", "Show help")
  private val specFile: OptionSpec[Path] = options.nonOptions("The modlist file").withValuesConvertedBy(new PathConverter())
  
  private val set: OptionSet = options.parse(args: _*)
  if (set.has(specHelp) || set.valuesOf(specFile).isEmpty) {
    options.printHelpOn(System.out)
  } else if (set.valuesOf(specFile).size() != 1) {
    System.err.println("Moonstone can only open a single file at once.")
    System.exit(1)
  } else {

    try {
      Class.forName("com.formdev.flatlaf.FlatLaf")
      System.setProperty("flatlaf.uiScale", "1")
      if (set.has(specLight)) {
        UIManager.setLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf")
      } else {
        UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf")
      }
    } catch {
      case _: ClassNotFoundException =>
      case e: Exception => e.printStackTrace()
    }
    
    val file = set.valueOf(specFile).toAbsolutePath.normalize()
    if (!Files.exists(file)) {
      Files.writeString(file, "{}", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }
    MoonStoneComponent.create(DesktopProject, new PathAccess(file), () => {}) match {
      case None => JOptionPane.showMessageDialog(null, "Failed to read modlist content.", "Moonstone error", JOptionPane.ERROR_MESSAGE, null)
      case Some(content) =>
        val frame = new JFrame()
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        frame.addWindowListener(new WindowAdapter {
          override def windowClosed(e: WindowEvent): Unit = {
            content.destroy()
            System.exit(0)
          }
        })

        frame.add(content)
        frame.setPreferredSize(new Dimension(1152, 896))
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.setVisible(true)
    }
  }
}
