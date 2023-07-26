package org.moddingx.moonstone.util

import com.google.gson.{JsonElement, JsonPrimitive}
import org.moddingx.moonstone.model.FileEntry
import org.moddingx.moonstone.platform.{FileDependency, ProjectDependency, ResolvableDependency}

// Need to treat dependencies on FAPI as QFAPI for quilt.
class QuiltDependencyHelper(fabricApi: JsonElement, quiltFabricApi: JsonElement) {
  
  def transform(dependency: ResolvableDependency): ResolvableDependency = dependency match {
    case ProjectDependency(this.fabricApi, modList, side) => ProjectDependency(this.quiltFabricApi, modList, side)
    // We can't get a file id for QFAPI, make it a project dependency
    case FileDependency(FileEntry(this.fabricApi, _, _, _), modList, side) => ProjectDependency(this.quiltFabricApi, modList, side)
    case _ => dependency
  }
}
