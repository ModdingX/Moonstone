package org.moddingx.moonstone.loader

import com.google.gson.JsonElement
import org.moddingx.moonstone.LoaderConstants
import org.moddingx.moonstone.model.{FileEntry, FileListAccess}
import org.moddingx.moonstone.platform.{ModdingPlatform, ResolvableDependency}
import org.moddingx.moonstone.util.DependencyHelper

trait QuiltedFabricLoaderCompatibility extends LoaderHelper {
  
  override def additionalSupportedLoaders(platform: ModdingPlatform, fileList: FileListAccess): Set[String] = {
    Set(LoaderConstants.Fabric) | super.additionalSupportedLoaders(platform, fileList)
  }

  override def transformDependency(platform: ModdingPlatform, fileList: FileListAccess, dependency: ResolvableDependency): ResolvableDependency = {
    val transformedDependency = super.transformDependency(platform, fileList, dependency)
    (platform.constants.fabricApi, platform.constants.quiltFabricApi) match {
      case (Some(fabricApi), Some(quiltFabricApi)) => DependencyHelper.redirect(transformedDependency, fabricApi -> quiltFabricApi)
      case _ => transformedDependency
    }
  }

  override def extraInformation(platform: ModdingPlatform, fileList: FileListAccess, project: JsonElement, file: Option[FileEntry]): Option[String] = {
    platform.constants.fabricApi match {
      case Some(fabricApi) if project == fabricApi => Some("Use Quilted Fabric API instead.")
      case _ => super.extraInformation(platform, fileList, project, file)
    }
  }
}
