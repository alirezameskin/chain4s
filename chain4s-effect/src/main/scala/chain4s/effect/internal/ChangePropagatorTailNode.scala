package chain4s.effect.internal

import cats.implicits._
import cats.effect.{ContextShift, IO, Timer}
import chain4s.internal.Logger
import chain4s.rpc.member.RpcClient
import chain4s.{LogEntry, SpeculativeLog}
import fs2.concurrent.{NoneTerminatedQueue, SignallingRef}
import retry.retryingOnAllErrors

class ChangePropagatorTailNode(
  log: SpeculativeLog[IO],
  client: RpcClient[IO],
  queue: NoneTerminatedQueue[IO, Long],
  interrupter: SignallingRef[IO, Boolean]
)(implicit val logger: Logger[IO], CS: ContextShift[IO], T: Timer[IO])
    extends ChangePropagatorImpl {

  override def propagateWrite(entry: LogEntry): IO[Unit] =
    IO.unit

  override def propagateCommit(index: Long): IO[Unit] =
    queue.enqueue1(Some(index))

  override def start: IO[Unit] = {
    val task = for {
      index <- log.getCommitIndex
      _     <- queue.enqueue1(Some(index))
      _     <- queue.dequeue.evalMap(client.commit).interruptWhen(interrupter).compile.drain
    } yield ()

    retryingOnAllErrors[Unit](policy = policy, onError = logError("ChangePropagation"))(task).start >> IO.unit
  }

  override def stop: IO[Unit] =
    interrupter.set(true)
}
