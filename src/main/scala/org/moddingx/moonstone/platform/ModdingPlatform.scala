package org.moddingx.moonstone.platform

import org.moddingx.moonstone.model.FileEntry
import java.util.Locale

trait ModdingPlatform {
  
  val name: String
  
  def validateEntry(entry: FileEntry): Option[FileEntry] = Some(entry)
}

object ModdingPlatform {
  
  val CURSE: ModdingPlatform = ??? // TODO
  val MODRINTH: ModdingPlatform = ??? // TODO
  
  def get(str: String): ModdingPlatform = str.toLowerCase(Locale.ROOT) match {
    case MODRINTH.name => MODRINTH
    case _ => CURSE // Curse is default for historical reasons
  }
}
