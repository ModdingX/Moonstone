package org.moddingx.moonstone.logic

import java.io.{InputStream, OutputStream}

trait FileAccess {
  def openForReading(requestor: AnyRef): InputStream
  def openForWriting(requestor: AnyRef): OutputStream
}

