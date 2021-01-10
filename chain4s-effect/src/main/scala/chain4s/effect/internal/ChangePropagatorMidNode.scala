package chain4s.effect.internal

import cats.implicits._
import cats.effect.{ContextShift, IO, Timer}
import chain4s.internal.Logger
import chain4s.rpc.member.RpcClient
import chain4s.{LogEntry, SpeculativeLog}
import fs2.concurrent.{Queue, SignallingRef}
import retry.retryingOnAllErrors

class ChangePropagatorMidNode(
  log: SpeculativeLog[IO],
  predecessorClient: RpcClient[IO],
  predecessorQueue: Queue[IO, Long],
  successorClient: RpcClient[IO],
  successorQueue: Queue[IO, LogEntry],
  interrupter: SignallingRef[IO, Boolean]
)(implicit val logger: Logger[IO], CS: ContextShift[IO], T: Timer[IO])
    extends ChangePropagatorImpl {

  override def propagateWrite(entry: LogEntry): IO[Unit] =
    successorQueue.enqueue1(entry)

  override def propagateCommit(index: Long): IO[Unit] =
    predecessorQueue.enqueue1(index)

  override def start: IO[Unit] = {
    val task = for {
      _        <- predecessorQueue.dequeue.evalMap(predecessorClient.commit).interruptWhen(interrupter).compile.drain.start
      _        <- successorQueue.dequeue.evalMap(successorClient.send).interruptWhen(interrupter).compile.drain.start
      index    <- log.getCommitIndex
      _        <- predecessorQueue.enqueue1(index)
      last     <- successorClient.last
      iterator <- log.entriesAfter(last)
      _        <- fs2.Stream.fromIterator[IO](iterator).through(successorQueue.enqueue).compile.drain
    } yield ()

    retryingOnAllErrors[Unit](policy = policy, onError = logError("ChangePropagator"))(task).start >> IO.unit
  }

  override def stop: IO[Unit] =
    interrupter.set(true)
}
