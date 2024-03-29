package org.moddingx.moonstone.display

import org.moddingx.moonstone.Util
import org.moddingx.moonstone.display.part.{ButtonHelper, JImage}
import org.moddingx.moonstone.model.Side

import java.awt.event.ActionEvent
import java.awt.{Color, Cursor, Dimension}
import javax.swing.SpringLayout.{BASELINE, EAST, NORTH, SOUTH, WEST}
import javax.swing._
import scala.collection.mutable.ListBuffer

class ModComponent(unit: ModUnit) extends JPanel {

  private val spring = new SpringLayout
  setLayout(spring)

  private val logo = new JImage(() => unit.image)
  add(logo)
  unit.url match {
    case Some(url) =>
      logo.setCursor(new Cursor(Cursor.HAND_CURSOR))
      logo.addActionListener((_: ActionEvent) => unit.project.openInBrowser(url))
    case None =>
  }
  unit.addImageResolveListener(() => {
    // We might be called at any time where we are invalid again
    if (this.isValid) {
      Util.dispatch {
        logo.repaint()
      }
    }
  })

  spring.putConstraint(WEST, logo, 3, WEST, this)
  spring.putConstraint(NORTH, logo, 3, NORTH, this)
  spring.putConstraint(SOUTH, logo, -3, SOUTH, this)

  private val buttonList: Seq[ButtonHelper.ButtonFactory] = if (unit.isSimple) {
    if (!unit.isInstalled) {
      List(ButtonHelper.DefaultButton(unit.project.swingFactory, "Install", enabled = true, _ => unit.install()))
    } else {
      Nil
    }
  } else {
    val updateButton = if (unit.canUpdate) {
      if (unit.isVersionLocked) {
        ButtonHelper.DefaultButton(unit.project.swingFactory, "Update Locked", enabled = false, _ => ())
      } else {
        ButtonHelper.DefaultButton(unit.project.swingFactory, "Update", enabled = true, _ => unit.update())
      }
    } else {
      ButtonHelper.DefaultButton(unit.project.swingFactory, "Up To Date", enabled = false, _ => ())
    }
    val lockButton = if (unit.isVersionLocked) {
      ButtonHelper.DefaultButton(unit.project.swingFactory, "Unlock Version", enabled = true, _ => unit.unlock())
    } else {
      ButtonHelper.DefaultButton(unit.project.swingFactory, "Lock Version", enabled = true, _ => {
        val suggestion = unit.versionLockSuggestion.getOrElse("")
        val name = unit.project.swingFactory.showInput("Lock Version", "Enter version id:", Some(suggestion))
        Option(name) match {
          case Some(input) => unit.lock(input)
          case None =>
        }
      })
    }
    val installButton = if (unit.isInstalled) {
      ButtonHelper.DefaultButton(unit.project.swingFactory, "Uninstall", enabled = true, _ => unit.uninstall())
    } else {
      ButtonHelper.DefaultButton(unit.project.swingFactory, "Install", enabled = true, _ => unit.install())
    }
    val sideButton = if (unit.canSetSide) {
      Some(ButtonHelper.SelectButton[Side](unit.project.swingFactory, unit.side, Side.values, _.id, (s, _) => {
        unit.setSide(s)
        s
      }))
    } else {
      None
    }
    Seq(updateButton, lockButton, installButton).appendedAll(sideButton)
  }

  private val buttons = if (buttonList.nonEmpty) {

    val topButton = buttonList.head.createButton()
    add(topButton)
    spring.putConstraint(NORTH, topButton, 3, NORTH, this)
    spring.putConstraint(EAST, topButton, -3, EAST, this)

    val buttonBuilder = ListBuffer[JComponent]()
    buttonBuilder.addOne(topButton)

    var lastButton = topButton
    for (definition <- buttonList.tail) {
      val button = definition.createButton()
      add(button)
      spring.putConstraint(NORTH, button, 1, SOUTH, lastButton)
      spring.putConstraint(EAST, button, 0, EAST, topButton)
      buttonBuilder.addOne(button)
      lastButton = button
    }

    // Set west value for all buttons based on the largest one
    val largestButton = buttonBuilder.maxBy(b => b.getPreferredSize.width)
    buttonBuilder.filter(b => b != largestButton).foreach(b => spring.putConstraint(WEST, b, 0, WEST, largestButton))

    buttonBuilder.result()
  } else {
    Nil
  }
  
  private val distribution = new JLabel()
  private val allowsThirdPartyDownloads = unit.allowsThirdPartyDownloads
  if (!allowsThirdPartyDownloads) {
    distribution.setText(Character.toString(0x1F6C7))
    distribution.setToolTipText("3rd party access disabled.")
  }
  add(distribution)
  distribution.setFont(distribution.getFont.deriveFont(distribution.getFont.getSize2D * 1.6f))
  distribution.setForeground(Color.RED)
  spring.putConstraint(WEST, distribution, 10, EAST, logo)
  
  private val extraInfo = new JLabel()
  private val hasExtraInformation = unit.extraInformation match {
    case Some(info) =>
      extraInfo.setText(Character.toString(0x1F6C8))
      extraInfo.setToolTipText(info)
      true
    case None => false
  }
  add(extraInfo)
  extraInfo.setFont(extraInfo.getFont.deriveFont(extraInfo.getFont.getSize2D * 1.6f))
  extraInfo.setForeground(new Color(63, 63, 255))
  spring.putConstraint(WEST, extraInfo, if (allowsThirdPartyDownloads) 0 else 5, EAST, distribution)

  private val title = new JLabel(unit.name)
  add(title)
  title.setFont(title.getFont.deriveFont(title.getFont.getSize2D * 1.75f))
  spring.putConstraint(WEST, title, if (hasExtraInformation) 5 else 0, EAST, extraInfo)
  spring.putConstraint(NORTH, title, 0, NORTH, logo)
  buttons.headOption.foreach(b => spring.putConstraint(EAST, title, -12, WEST, b))

  spring.putConstraint(BASELINE, distribution, 0, BASELINE, title)
  spring.putConstraint(BASELINE, extraInfo, 0, BASELINE, title)

  private val versionStr = new JLabel(unit.version.getOrElse(""))
  add(versionStr)
  spring.putConstraint(WEST, versionStr, 10, EAST, logo)
  spring.putConstraint(NORTH, versionStr, 3, SOUTH, title)
  buttons.headOption.foreach(b => spring.putConstraint(EAST, versionStr, -12, WEST, b))

  // html tags required so it is multiline.
  private val description = new JLabel("<html>" + unit.description + "</html>")
  description.setVerticalAlignment(SwingConstants.TOP)
  add(description)
  spring.putConstraint(WEST, description, 10, EAST, logo)
  spring.putConstraint(NORTH, description, 3, SOUTH, versionStr)
  spring.putConstraint(SOUTH, description, -3, SOUTH, this)
  buttons.headOption.foreach(b => spring.putConstraint(EAST, description, -12, WEST, b))
  description.setPreferredSize(new Dimension(1, 1))

  def initSize(initSize: Int): Unit = {
    logo.setPreferredSize(new Dimension(initSize, initSize))
    setPreferredSize(calculatePreferredSize(initSize))
  }

  def suggestedInitSize: Int = {
    val allTextHeight = title.getPreferredSize.height max versionStr.getPreferredSize.height max description.getPreferredSize.height
    val allButtonHeight = if (buttons.isEmpty) 0 else buttons.map(_.getPreferredSize.height).sum + (buttons.size - 1)
    80 max allTextHeight max allButtonHeight
  }

  private var lastCalculatedSize: Option[(Int, Int, Int)] = None

  def calculatePreferredSize(initSize: Int): Dimension = {
    if (lastCalculatedSize.isDefined && lastCalculatedSize.get._1 == initSize) {
      return new Dimension(lastCalculatedSize.get._2, lastCalculatedSize.get._3)
    }

    val logoWidth = initSize
    val logoHeight = initSize

    val allTextWidth = title.getPreferredSize.width max versionStr.getPreferredSize.width max description.getPreferredSize.width
    val allTextHeight = title.getPreferredSize.height max versionStr.getPreferredSize.height max description.getPreferredSize.height

    val allButtonWidth = buttons.map(_.getPreferredSize.width).maxOption.getOrElse(0)
    val allButtonHeight = if (buttons.isEmpty) 0 else buttons.map(_.getPreferredSize.height).sum + (buttons.size - 1)

    val totalWidth = 3 + logoWidth + 10 + allTextWidth + 12 + allButtonWidth + 3
    val totalHeight = 3 + (logoHeight max allTextHeight max allButtonHeight) + 3

    lastCalculatedSize = Some((initSize, totalWidth, totalHeight))
    new Dimension(totalWidth, totalHeight)
  }
}
