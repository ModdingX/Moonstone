package io.github.noeppi_noeppi.tools.moonstone.display

import com.intellij.openapi.project.Project
import io.github.noeppi_noeppi.tools.moonstone.model.Side

import java.awt.image.BufferedImage
import java.net.URL

trait ModUnit {

  def project: Project
  def name: String
  def version: Option[String]
  def description: String
  def image(): Option[BufferedImage]
  def url(): Option[URL]
  def side(): Side
  def addImageResolveListener(listener: () => Unit): Unit
  def versionLockSuggestion(): Option[Int]

  def isSimple: Boolean
  def isInstalled: Boolean
  def canUpdate: Boolean
  def isVersionLocked: Boolean
  def canSetSide: Boolean

  def install(): Unit
  def uninstall(): Unit
  def update(): Unit
  def lock(fileId: Int): Unit
  def unlock(): Unit
  def setSide(side: Side): Unit

  def resolve(): Unit
}
