package org.moddingx.moonstone.platform.modrinth

import com.google.gson.{JsonElement, JsonPrimitive, JsonSyntaxException}
import org.moddingx.moonstone.model.FileEntry
import org.moddingx.moonstone.platform.{ModList, ModdingPlatform, PlatformAccess, PlatformConstants}

object ModrinthPlatform extends ModdingPlatform {
  
  override val name: String = "modrinth"

  object constants extends PlatformConstants {
    override val fabricApi: Option[JsonElement] = Some(new JsonPrimitive("P7dR8mSH"))
    override val quiltFabricApi: Option[JsonElement] = Some(new JsonPrimitive("qvIfYCYJ"))
    override val sinytraConnector: Option[JsonElement] = Some(new JsonPrimitive("u58R1TMW"))
    override val sinytraFabricApi: Option[JsonElement] = Some(new JsonPrimitive("Aqlf1Shp"))
  }

  override def createAccess(list: ModList): PlatformAccess = new ModrinthAccess(list)

  override protected def validateVersion(file: FileEntry): Option[FileEntry] = {
    try {
      Some(FileEntry(
        new JsonPrimitive(file.project.getAsString),
        new JsonPrimitive(file.file.getAsString),
        file.side, file.locked
      ))
    } catch {
      case _: JsonSyntaxException => None
    }
  }
}
