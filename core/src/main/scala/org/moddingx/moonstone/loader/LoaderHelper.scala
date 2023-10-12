package org.moddingx.moonstone.loader

import com.google.gson.JsonElement
import org.moddingx.moonstone.LoaderConstants
import org.moddingx.moonstone.model.{FileEntry, FileListAccess}
import org.moddingx.moonstone.platform.{ModdingPlatform, ResolvableDependency}

trait LoaderHelper {

  def additionalSupportedLoaders(platform: ModdingPlatform, fileList: FileListAccess): Set[String] = Set()
  def transformDependency(platform: ModdingPlatform, fileList: FileListAccess, dependency: ResolvableDependency): ResolvableDependency = dependency
  def extraInformation(platform: ModdingPlatform, fileList: FileListAccess, project: JsonElement, file: Option[FileEntry]): Option[String] = None
}

object LoaderHelper {
  def apply(loader: String): LoaderHelper = loader match {
    case LoaderConstants.Forge => ForgeLoaderHelper
    case LoaderConstants.Quilt => QuiltLoaderHelper
    case LoaderConstants.NeoForge => NeoForgeLoaderHelper
    case _ => NoopLoaderHelper
  }
  
  private object NoopLoaderHelper extends LoaderHelper
}
