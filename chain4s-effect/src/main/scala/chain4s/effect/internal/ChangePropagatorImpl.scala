package chain4s.effect.internal

import cats.effect.{ContextShift, IO, Timer}
import chain4s.internal.{ChangePropagator, Logger}
import chain4s.rpc.RpcClientBuilder
import chain4s.{LogEntry, Node, SpeculativeLog}
import fs2.concurrent.{Queue, SignallingRef}
import retry.RetryDetails.{GivingUp, WillDelayAndRetry}
import retry._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait ChangePropagatorImpl extends ChangePropagator[IO] {
  implicit val logger: Logger[IO]
  val policy = RetryPolicies.constantDelay[IO](FiniteDuration(500, TimeUnit.MILLISECONDS))

  def logError(action: String)(err: Throwable, details: RetryDetails): IO[Unit] = details match {
    case WillDelayAndRetry(nextDelay: FiniteDuration, retriesSoFar: Int, cumulativeDelay: FiniteDuration) =>
      logger.error(
        s"Error has occurred on $action, retry[counter=$retriesSoFar] after ${nextDelay.toMillis} [ms] sleeping..., total delay was ${cumulativeDelay.toMillis} [ms] so far"
      )

    case GivingUp(totalRetries: Int, totalDelay: FiniteDuration) =>
      logger.error(s"Giving up on $action after $totalRetries retries, finally total delay was ${totalDelay.toMillis} [ms]")
  }
}

object ChangePropagatorImpl {
  def build(log: SpeculativeLog[IO], predecessor: Option[Node], successor: Option[Node])(implicit
    CS: ContextShift[IO],
    T: Timer[IO],
    L: Logger[IO],
    builder: RpcClientBuilder[IO]
  ): IO[ChangePropagator[IO]] =
    (predecessor, successor) match {
      case (None, None) =>
        IO(new ChangePropagatorNull)

      case (Some(pred), Some(succ)) =>
        for {
          predQueue <- Queue.unbounded[IO, Long]
          succQueue <- Queue.unbounded[IO, LogEntry]
          predClient = builder.build(pred)
          succClient = builder.build(succ)
          predFiber <- predQueue.dequeue.evalMap(index => predClient.commit(index)).compile.drain.start
          succFiber <- succQueue.dequeue.evalMap(entry => succClient.send(entry)).compile.drain.start
          signal    <- SignallingRef[IO, Boolean](false)
        } yield new ChangePropagatorMidNode(log, predClient, predQueue, succClient, succQueue, signal)

      case (Some(pred), None) =>
        for {
          queue <- Queue.noneTerminated[IO, Long]
          client = builder.build(pred)
          signal <- SignallingRef[IO, Boolean](false)
        } yield new ChangePropagatorTailNode(log, client, queue, signal)

      case (None, Some(succ)) =>
        for {
          queue <- Queue.unbounded[IO, LogEntry]
          client = builder.build(succ)
          signal <- SignallingRef[IO, Boolean](false)
        } yield new ChangePropagatorHeadNode(log, client, queue, signal)
    }

}
