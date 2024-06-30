package org.moddingx.moonstone.intellij

import com.intellij.openapi.vfs.VirtualFile
import org.moddingx.moonstone.logic.FileAccess

import java.io.{InputStream, OutputStream}

class IntellijFile(file: VirtualFile) extends FileAccess {
  
  override def openForReading(requestor: AnyRef): InputStream = file.getInputStream
  override def openForWriting(requestor: AnyRef): OutputStream = file.getOutputStream(requestor)
}
