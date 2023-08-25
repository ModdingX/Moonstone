package org.moddingx.moonstone.platform.curse

import com.google.gson.{JsonPrimitive, JsonSyntaxException}
import org.moddingx.moonstone.model.FileEntry
import org.moddingx.moonstone.platform.{ModList, ModdingPlatform, PlatformAccess}

object CursePlatform extends ModdingPlatform {

  override val name: String = "curseforge"

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
