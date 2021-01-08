package chain4s.effect

import cats.effect.{ContextShift, IO, Resource, Timer}
import chain4s.internal.Logger
import chain4s.{Cluster, ClusterConfiguration, Configuration, SpeculativeLog, StableLog}
import chain4s.rpc.{RpcClientBuilder, RpcServerBuilder}

object ReplicationCluster {
  def resource(config: Configuration, speculativeLog: SpeculativeLog[IO], stableLog: StableLog[IO])(implicit
    CS: ContextShift[IO],
    SB: RpcServerBuilder[IO],
    CB: RpcClientBuilder[IO],
    T: Timer[IO],
    L: Logger[IO]
  ): Resource[IO, Cluster[IO]] =
    for {
      replication <- Resource.liftF(
        ChainReplicationImpl.build(config.local, ClusterConfiguration(config.nodes), speculativeLog, stableLog)
      )
      rpcServer <- Resource.liftF(RpcServerBuilder[IO].build(config.local, replication))
      cluster   <- Resource.make(IO(new Cluster[IO](replication, rpcServer)))(_.stop)
    } yield cluster
}
