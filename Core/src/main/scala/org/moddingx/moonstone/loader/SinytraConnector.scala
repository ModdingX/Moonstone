package org.moddingx.moonstone.loader

import com.google.gson.JsonElement
import org.moddingx.moonstone.LoaderConstants
import org.moddingx.moonstone.model.{FileEntry, FileListAccess}
import org.moddingx.moonstone.platform.{ModdingPlatform, ResolvableDependency}
import org.moddingx.moonstone.util.DependencyHelper

trait SinytraConnector extends LoaderHelper {

  override def additionalSupportedLoaders(platform: ModdingPlatform, fileList: FileListAccess): Set[String] = {
    val sinytraSupport: Set[String] = platform.constants.sinytraConnector match {
      case Some(sinytraConnector) if fileList.hasInstalledProject(sinytraConnector) => Set(LoaderConstants.Fabric)
      case _ => Set()
    }
    sinytraSupport | super.additionalSupportedLoaders(platform, fileList)
  }

  override def transformDependency(platform: ModdingPlatform, fileList: FileListAccess, dependency: ResolvableDependency): ResolvableDependency = {
    val transformedDependency = super.transformDependency(platform, fileList, dependency)
    (platform.constants.sinytraConnector, platform.constants.fabricApi, platform.constants.sinytraFabricApi) match {
      case (Some(sinytraConnector), Some(fabricApi), Some(sinytraFabricApi)) if fileList.hasInstalledProject(sinytraConnector) => DependencyHelper.redirect(transformedDependency, fabricApi -> sinytraFabricApi)
      case _ => transformedDependency
    }
  }
  override def extraInformation(platform: ModdingPlatform, fileList: FileListAccess, project: JsonElement, file: Option[FileEntry]): Option[String] = {
    (platform.constants.sinytraConnector, platform.constants.fabricApi) match {
      case (Some(sinytraConnector), _) if project == sinytraConnector => Some("Allows installing fabric mods.")
      case (Some(sinytraConnector), Some(fabricApi)) if fileList.hasInstalledProject(sinytraConnector) && project == fabricApi => Some("Use Forgified Fabric API instead.")
      case _ => super.extraInformation(platform, fileList, project, file)
    }
  }
}
