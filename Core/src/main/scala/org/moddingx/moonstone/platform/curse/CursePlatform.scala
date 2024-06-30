package org.moddingx.moonstone.platform.curse

import com.google.gson.{JsonElement, JsonPrimitive, JsonSyntaxException}
import org.moddingx.moonstone.model.FileEntry
import org.moddingx.moonstone.platform.{ModList, ModdingPlatform, PlatformAccess, PlatformConstants}

object CursePlatform extends ModdingPlatform {

  override val name: String = "curseforge"

  object constants extends PlatformConstants {
    override val fabricApi: Option[JsonElement] = Some(new JsonPrimitive(306612))
    override val quiltFabricApi: Option[JsonElement] = Some(new JsonPrimitive(634179))
    override val sinytraConnector: Option[JsonElement] = Some(new JsonPrimitive(890127))
    override val sinytraFabricApi: Option[JsonElement] = Some(new JsonPrimitive(889079))
  }
  
  override def createAccess(list: ModList): PlatformAccess = new CurseAccess(list)
  
  override protected def validateVersion(file: FileEntry): Option[FileEntry] = {
    try {
      Some(FileEntry(
        new JsonPrimitive(file.project.getAsInt),
        new JsonPrimitive(file.file.getAsInt),
        file.side, file.locked
      ))
    } catch {
      case _: JsonSyntaxException => None
    }
  }
}
