package org.moddingx.moonstone.loader

import com.google.gson.JsonElement
import org.moddingx.moonstone.LoaderConstants
import org.moddingx.moonstone.model.{FileEntry, FileListAccess}
import org.moddingx.moonstone.platform.{ModdingPlatform, ResolvableDependency}

abstract class LoaderHelper {
  def additionalSupportedLoaders(platform: ModdingPlatform, fileList: FileListAccess): Set[String] = Set()
  def transformDependency(platform: ModdingPlatform, fileList: FileListAccess, dependency: ResolvableDependency): ResolvableDependency = dependency
  def extraInformation(platform: ModdingPlatform, fileList: FileListAccess, project: JsonElement, file: Option[FileEntry]): Option[String] = None
}

object LoaderHelper {
  def apply(loader: String): LoaderHelper = loader match {
    case LoaderConstants.Forge => Forge
    case LoaderConstants.Quilt => Quilt
    case LoaderConstants.NeoForge => NeoForge
    case _ => Noop
  }
  
  private object Noop extends LoaderHelper
  private object Forge extends LoaderHelper with SinytraConnector
  private object Quilt extends LoaderHelper with QuiltedFabricLoaderCompatibility
  private object NeoForge extends LoaderHelper with NeoForgeMainlineCompatibility with SinytraConnector
}
