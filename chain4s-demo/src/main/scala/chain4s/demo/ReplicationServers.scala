package chain4s.demo

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits._
import chain4s.effect.grpc.reply.GrpcReplyClientBuilder
import chain4s.effect.grpc.member.{GrpcClientBuilder, GrpcServerBuilder}
import chain4s.effect.{ReplicationCluster, SpeculativeLogImpl}
import chain4s.grpc.serializer.JavaSerializer
import chain4s.internal.Logger
import chain4s.{Cluster, ClusterConfiguration, Configuration}

class ReplicationServers(clusters: Seq[Cluster[IO]])(implicit logger: Logger[IO]) {
  def start: IO[Unit] =
    for {
      _ <- logger.trace("Starting Cluster members")
      _ <- clusters.toList.traverse(_.start)
    } yield ()

  def stop: IO[Unit] =
    for {
      _ <- logger.trace("Stopping Cluster members")
      _ <- clusters.toList.traverse(_.stop)
    } yield ()
}

object ReplicationServers {

  def build(config: ClusterConfiguration)(implicit CS: ContextShift[IO], T: Timer[IO], L: Logger[IO]) =
    config.nodes
      .map(n => Configuration(n, config.nodes))
      .traverse(makeCluster)
      .flatMap { clusters =>
        Resource.make(IO(new ReplicationServers(clusters)))(_.stop)
      }

  private def makeCluster(config: Configuration)(implicit CS: ContextShift[IO], T: Timer[IO], L: Logger[IO]) = {

    implicit val serializer         = new JavaSerializer
    implicit val clientBuilder      = new GrpcClientBuilder()
    implicit val serverBuilder      = new GrpcServerBuilder()
    implicit val replyClientBuilder = new GrpcReplyClientBuilder()

    for {
      log       <- Resource.liftF(SpeculativeLogImpl.empty)
      stableLog <- Resource.liftF(StableLogImpl.empty())
      cluster   <- ReplicationCluster.resource(config, log, stableLog)
    } yield cluster

  }
}
