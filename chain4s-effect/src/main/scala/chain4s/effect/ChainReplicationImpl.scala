package chain4s.effect

import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, IO, Timer}
import chain4s.effect.internal.{ChangePropagatorImpl, ReplierImpl}
import chain4s.internal._
import chain4s.rpc.RpcClientBuilder
import chain4s._

class ChainReplicationImpl(
  val local: Node,
  val speculativeLog: SpeculativeLog[IO],
  val stableLog: StableLog[IO],
  val replier: Replier[IO],
  stateRef: Ref[IO, Replica],
  val propagatorRef: Ref[IO, ChangePropagator[IO]]
)(implicit
  val logger: Logger[IO],
  CS: ContextShift[IO],
  T: Timer[IO],
  CB: RpcClientBuilder[IO]
) extends ChainReplication[IO] {

  override def start: IO[Unit] =
    for {
      propagator <- propagatorRef.get
      _          <- propagator.start
    } yield ()

  override def stop: IO[Unit] =
    for {
      propagator <- propagatorRef.get
      _          <- propagator.stop
    } yield ()

  override def getCurrentState: IO[Replica] =
    stateRef.get

  override def setCurrentState(state: Replica): IO[Unit] =
    for {
      _ <- stateRef.set(state)
      _ <- propagatorRef.get.map(_.stop)
      p <- ChangePropagatorImpl.build(speculativeLog, state.predecessor, state.successor)
      _ <- propagatorRef.set(p)
    } yield ()

  override def propagateWrite(entry: LogEntry): IO[Unit] =
    for {
      propagator <- propagatorRef.get
      _          <- propagator.propagateWrite(entry)
    } yield ()

  override def propagateCommit(index: Long): IO[Unit] =
    for {
      propagator <- propagatorRef.get
      _          <- propagator.propagateCommit(index)
    } yield ()

}

object ChainReplicationImpl {

  def build(node: Node, config: ClusterConfiguration, speculativeLog: SpeculativeLog[IO], stableLog: StableLog[IO])(implicit
    CS: ContextShift[IO],
    T: Timer[IO],
    L: Logger[IO],
    CB: RpcClientBuilder[IO]
  ): IO[ChainReplicationImpl] =
    for {
      replier    <- IO(new ReplierImpl)
      replica    <- IO(ChainReplication.detectReplica(node, config.nodes.toList))
      replicaRef <- Ref.of[IO, Replica](replica)
      propagator <- ChangePropagatorImpl.build(speculativeLog, replica.predecessor, replica.successor)
      propRef    <- Ref.of[IO, ChangePropagator[IO]](propagator)
    } yield new ChainReplicationImpl(node, speculativeLog, stableLog, replier, replicaRef, propRef)
}
