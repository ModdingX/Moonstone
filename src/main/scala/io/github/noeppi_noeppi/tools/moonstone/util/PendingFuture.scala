package io.github.noeppi_noeppi.tools.moonstone.util

import java.util.concurrent.{Future, TimeUnit}

object PendingFuture extends Future[Nothing] {
  override def cancel(interrupt: Boolean): Boolean = false
  override def isCancelled: Boolean = false
  override def isDone: Boolean = false
  override def get(): Nothing = throw new NoSuchElementException
  override def get(timeout: Long, unit: TimeUnit): Nothing = throw new NoSuchElementException
  
  def instance[T]: Future[T] = PendingFuture.asInstanceOf[Future[T]]
}
