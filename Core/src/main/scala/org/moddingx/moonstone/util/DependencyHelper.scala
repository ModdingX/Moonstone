package org.moddingx.moonstone.util

import com.google.gson.JsonElement
import org.moddingx.moonstone.model.{FileEntry, Side}
import org.moddingx.moonstone.platform.{FileDependency, ProjectDependency, ResolvableDependency}

object DependencyHelper {
  
  def redirect(dependency: ResolvableDependency, redirects: (JsonElement, JsonElement)*): ResolvableDependency = redirect(dependency, Map(redirects: _*))
  def redirect(dependency: ResolvableDependency, redirects: Map[JsonElement, JsonElement]): ResolvableDependency = {
    dependency match {
      case ProjectDependency(project, modList, side) => redirects.get(project) match {
        case Some(newProject) => ProjectDependency(newProject, modList, side)
        case _ => dependency
      }
      case FileDependency(FileEntry(project, _, _, _), modList, side) => redirects.get(project) match {
        case Some(newProject) => ProjectDependency(newProject, modList, side)
        case _ => dependency
      }
    }
  }
}
