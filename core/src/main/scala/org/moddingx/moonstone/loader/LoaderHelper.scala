package org.moddingx.moonstone.loader

import com.google.gson.JsonPrimitive
import org.moddingx.moonstone.LoaderConstants
import org.moddingx.moonstone.model.FileEntry
import org.moddingx.moonstone.platform.curse.CursePlatform
import org.moddingx.moonstone.platform.modrinth.ModrinthPlatform
import org.moddingx.moonstone.platform.{FileDependency, ModdingPlatform, ProjectDependency, ResolvableDependency}

trait LoaderHelper {

  def additionalSupportedLoaders(minecraft: String): Set[String] = Set()
  def transformDependency(platform: ModdingPlatform, dependency: ResolvableDependency): ResolvableDependency = dependency
}

object LoaderHelper {
  def apply(loader: String): LoaderHelper = loader match {
    case LoaderConstants.Quilt => QuiltLoaderHelper
    case _ => NoopLoaderHelper
  }
  
  private object NoopLoaderHelper extends LoaderHelper
  private object QuiltLoaderHelper extends LoaderHelper {
    override def additionalSupportedLoaders(minecraft: String): Set[String] = Set(LoaderConstants.Fabric)
    override def transformDependency(platform: ModdingPlatform, dependency: ResolvableDependency): ResolvableDependency = {
      val (fabricApi: JsonPrimitive, quiltFabricApi: JsonPrimitive) = platform match {
        case CursePlatform => (new JsonPrimitive(306612), new JsonPrimitive(634179))
        case ModrinthPlatform => (new JsonPrimitive("P7dR8mSH"), new JsonPrimitive("qvIfYCYJ"))
        case _ => return dependency
      }
      dependency match {
        case ProjectDependency(`fabricApi`, modList, side) => ProjectDependency(quiltFabricApi, modList, side)
        // We can't get a file id for QFAPI, make it a project dependency
        case FileDependency(FileEntry(`fabricApi`, _, _, _), modList, side) => ProjectDependency(quiltFabricApi, modList, side)
        case _ => dependency
      }
    }
  }
}
