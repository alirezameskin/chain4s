package chain4s

import cats.Monad
import cats.implicits._
import chain4s.internal.{Logger, Replica, Replier}

abstract class ChainReplication[F[_]: Monad]() {
  val local: Node

  val logger: Logger[F]
  val replier: Replier[F]
  val speculativeLog: SpeculativeLog[F]
  val stableLog: StableLog[F]

  def getCurrentState: F[Replica]
  def setCurrentState(state: Replica): F[Unit]
  def propagateWrite(entry: LogEntry): F[Unit]
  def propagateCommit(index: Long): F[Unit]
  def start: F[Unit]
  def stop: F[Unit]

  def write(request: WriteRequest): F[Unit] =
    for {
      entry   <- speculativeLog.create(request)
      _       <- logger.trace(s"On ${local}: A Log entry created at index ${entry.index}")
      replica <- getCurrentState
      _       <- propagateOrCommit(replica, entry)
    } yield ()

  def read(request: ReadRequest): F[request.RESULT] =
    stableLog.query(request.command)

  def onAppend(entry: LogEntry): F[Acknowledgment] =
    speculativeLog.getLastIndex.flatMap {
      case entry.index                => Monad[F].pure(Accepted(local, entry.index))
      case last if entry.index < last => Monad[F].pure(Rejected(local, last))
      case _                          => doAppendRequest(entry)
    }

  def onCommit(index: Long): F[Unit] =
    for {
      cIndex  <- speculativeLog.getCommitIndex
      _       <- (cIndex + 1 to index).toList.traverse(commit)
      _       <- speculativeLog.setCommitIndex(index)
      replica <- getCurrentState

      _ <- replica.predecessor match {
        case Some(_) => propagateCommit(index)
        case None    => Monad[F].unit
      }
    } yield ()

  private def commit(index: Long): F[Unit] =
    for {
      entry <- speculativeLog.getEntry(index)
      _     <- stableLog.commit(entry.request.command)
      _     <- logger.trace(s"On ${local}: Log Entry at ${index} is commited.")
    } yield ()

  def onConfiguration(config: ClusterConfiguration): F[Unit] =
    setCurrentState(ChainReplication.detectReplica(local, config.nodes.toList))

  private def doAppendRequest(entry: LogEntry): F[Acknowledgment] =
    for {
      entry   <- speculativeLog.append(entry)
      _       <- logger.trace(s"On ${local}: Appending a LogEntry index: ${entry.index}")
      replica <- getCurrentState
      _       <- propagateOrCommit(replica, entry)
    } yield Accepted(local, entry.index)

  private def propagateOrCommit(replica: Replica, entry: LogEntry): F[Unit] =
    replica.successor match {
      case Some(_) =>
        propagateWrite(entry)

      case None =>
        for {
          result <- stableLog.commit(entry.request.command)
          _      <- speculativeLog.setCommitIndex(entry.index)
          _      <- replier.sendReply(entry.request)(result)
          _ <- replica.predecessor match {
            case Some(_) => propagateCommit(entry.index)
            case None    => Monad[F].unit
          }
        } yield ()
    }

}

object ChainReplication {
  def detectReplica(local: Node, nodes: List[Node], prev: Option[Node] = None): Replica =
    nodes match {
      case h :: Nil if h == local           => internal.Replica(h, prev, None)
      case h :: t :: _ if h == local        => internal.Replica(local, prev, Some(t))
      case h :: t :: Nil if t == local      => internal.Replica(local, Some(h), None)
      case h :: m :: t :: Nil if m == local => internal.Replica(local, Some(h), Some(t))
      case h :: tail                        => detectReplica(local, tail, Some(h))
      case _                                => internal.Replica(local, None, None) /// ???
    }
}
