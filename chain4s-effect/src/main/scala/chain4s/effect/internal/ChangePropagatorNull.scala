package chain4s.effect.internal

import cats.effect.IO
import chain4s.LogEntry
import chain4s.internal.ChangePropagator

class ChangePropagatorNull() extends ChangePropagator[IO] {
  override def propagateWrite(entry: LogEntry): IO[Unit] = IO.unit
  override def propagateCommit(index: Long): IO[Unit]    = IO.unit

  override def start: IO[Unit] = IO.unit
  override def stop: IO[Unit]  = IO.unit
}
