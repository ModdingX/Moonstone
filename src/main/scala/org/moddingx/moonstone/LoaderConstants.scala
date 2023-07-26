package org.moddingx.moonstone

import com.google.gson.JsonPrimitive
import org.moddingx.moonstone.util.QuiltDependencyHelper

object LoaderConstants {

  val Forge: String = "forge"
  val Fabric: String = "fabric"
  val Quilt: String = "quilt"
  
  val SuggestedLoaders: Seq[String] = Seq(Forge, Fabric, Quilt)
  
  val CurseQuiltHelper: QuiltDependencyHelper = new QuiltDependencyHelper(
    fabricApi = new JsonPrimitive(306612),
    quiltFabricApi = new JsonPrimitive(634179)
  )
  
  val ModrinthQuiltHelper: QuiltDependencyHelper = new QuiltDependencyHelper(
    fabricApi = new JsonPrimitive("P7dR8mSH"),
    quiltFabricApi = new JsonPrimitive("qvIfYCYJ")
  )
}
