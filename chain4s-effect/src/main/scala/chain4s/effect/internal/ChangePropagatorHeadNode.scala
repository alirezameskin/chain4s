package chain4s.effect.internal

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import chain4s.internal.Logger
import chain4s.rpc.member.RpcClient
import chain4s.{LogEntry, SpeculativeLog}
import fs2.concurrent.{Queue, SignallingRef}
import retry.retryingOnAllErrors

class ChangePropagatorHeadNode(
  log: SpeculativeLog[IO],
  client: RpcClient[IO],
  queue: Queue[IO, LogEntry],
  interrupter: SignallingRef[IO, Boolean]
)(implicit val logger: Logger[IO], CS: ContextShift[IO], T: Timer[IO])
    extends ChangePropagatorImpl {

  override def propagateWrite(entry: LogEntry): IO[Unit] =
    queue.enqueue1(entry)

  override def propagateCommit(index: Long): IO[Unit] =
    IO.unit

  override def start: IO[Unit] = {
    val task = for {
      last     <- client.last
      iterator <- log.entriesAfter(last)
      _        <- queue.dequeue.evalMap(entry => client.send(entry)).interruptWhen(interrupter).compile.drain.start
      _        <- fs2.Stream.fromIterator[IO](iterator).through(queue.enqueue).compile.drain
    } yield ()

    retryingOnAllErrors[Unit](policy = policy, onError = logError("ChangePropagation"))(task).start >> IO.unit
  }

  override def stop: IO[Unit] =
    interrupter.set(true)
}
