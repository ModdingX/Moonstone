package org.moddingx.moonstone.platform

import com.google.gson.JsonElement

trait PlatformConstants {
    val fabricApi: Option[JsonElement]
    val quiltFabricApi: Option[JsonElement]
    val sinytraConnector: Option[JsonElement]
    val sinytraFabricApi: Option[JsonElement]
}
