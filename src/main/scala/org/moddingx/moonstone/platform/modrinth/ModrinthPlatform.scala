package org.moddingx.moonstone.platform.modrinth

import com.google.gson.{JsonPrimitive, JsonSyntaxException}
import org.moddingx.moonstone.model.FileEntry
import org.moddingx.moonstone.platform.{ModList, ModdingPlatform, PlatformAccess}

object ModrinthPlatform extends ModdingPlatform {
  
  override val name: String = "modrinth"

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
