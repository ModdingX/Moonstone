package org.moddingx.moonstone.display

import com.intellij.openapi.project.Project
import org.moddingx.moonstone.model.Side

import java.awt.image.BufferedImage
import java.net.URI

trait ModUnit {

  def project: Project
  def name: String
  def version: Option[String]
  def description: String
  def image: Option[BufferedImage]
  def url: Option[URI]
  def side: Side
  def addImageResolveListener(listener: () => Unit): Unit
  
  def versionLockSuggestion: Option[String]
  def allowsThirdPartyDownloads: Boolean

  def isSimple: Boolean
  def isInstalled: Boolean
  def canUpdate: Boolean
  def isVersionLocked: Boolean
  def canSetSide: Boolean

  def install(): Unit
  def uninstall(): Unit
  def update(): Unit
  def lock(input: String): Unit
  def unlock(): Unit
  def setSide(side: Side): Unit

  def resolve(): Unit
}
