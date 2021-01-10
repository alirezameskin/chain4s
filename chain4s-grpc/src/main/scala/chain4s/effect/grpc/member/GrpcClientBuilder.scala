package chain4s.effect.grpc.member

import cats.effect.{ContextShift, IO}
import chain4s.Node
import chain4s.grpc.serializer.Serializer
import chain4s.rpc.member.{RpcClient, RpcClientBuilder}
import io.grpc.ManagedChannelBuilder

class GrpcClientBuilder(implicit S: Serializer, CS: ContextShift[IO]) extends RpcClientBuilder[IO] {
  override def build(node: Node): IO[RpcClient[IO]] = IO {

    val builder: ManagedChannelBuilder[_] = ManagedChannelBuilder
      .forAddress(node.address.host, node.address.port)
      .enableRetry()
      .usePlaintext()

    new GrpcClient(node, builder.build(), S)
  }
}
