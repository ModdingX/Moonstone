package org.moddingx.moonstone.loader

import org.moddingx.moonstone.LoaderConstants
import org.moddingx.moonstone.model.FileListAccess
import org.moddingx.moonstone.platform.ModdingPlatform

object NeoForgeLoaderHelper extends LoaderHelper {
  
  override def additionalSupportedLoaders(platform: ModdingPlatform, fileList: FileListAccess): Set[String] = fileList.mcVersion match {
    case "1.20.1" => Set(LoaderConstants.Forge)
    case _ => Set()
  }
}
