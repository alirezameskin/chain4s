package chain4s.internal

import chain4s.{LogEntry, Node}

trait ChangePropagator[F[_]] {

  def start: F[Unit]
  def stop: F[Unit]

  def propagateWrite(entry: LogEntry): F[Unit]

  def propagateCommit(index: Long): F[Unit]
}
