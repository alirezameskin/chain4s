package chain4s.demo

import cats.effect.{Concurrent, ContextShift, IO, Resource}
import chain4s.effect.grpc.member.GrpcClientBuilder
import chain4s.proxy.ReplicationProxy
import chain4s.effect.grpc.reply.GrpcReplyReceiverBuilder
import chain4s.grpc.serializer.JavaSerializer
import chain4s.{Address, ClusterConfiguration, Node}

object ProxyServer {

  def build(address: Address, config: ClusterConfiguration)(implicit
    C: Concurrent[IO],
    CS: ContextShift[IO]
  ): Resource[IO, ReplicationProxy[IO]] = {

    implicit val serializer    = new JavaSerializer
    implicit val clientBuilder = new GrpcClientBuilder()
    implicit val serverBuilder = new GrpcReplyReceiverBuilder()

    ReplicationProxy.build[IO](address, config)
  }
}
