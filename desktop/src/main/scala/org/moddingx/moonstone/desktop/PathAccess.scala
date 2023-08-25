package org.moddingx.moonstone.desktop

import org.moddingx.moonstone.logic.FileAccess

import java.io.{InputStream, OutputStream}
import java.nio.file.{Files, Path, StandardOpenOption}

class PathAccess(val path: Path) extends FileAccess {
  override def openForReading(requestor: AnyRef): InputStream = Files.newInputStream(path)
  override def openForWriting(requestor: AnyRef): OutputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
}
