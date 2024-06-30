package org.moddingx.moonstone

import com.google.gson.JsonPrimitive

object LoaderConstants {

  val Forge: String = "forge"
  val Fabric: String = "fabric"
  val Quilt: String = "quilt"
  val NeoForge: String = "neoforge"
  
  val SuggestedLoaders: Seq[String] = Seq(Forge, Fabric, Quilt, NeoForge)
}
