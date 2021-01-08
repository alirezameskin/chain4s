package chain4s.effect.grpc

import cats.effect.{ContextShift, IO}
import chain4s.Node
import chain4s.grpc.serializer.Serializer
import chain4s.rpc.{RpcClient, RpcClientBuilder}
import io.grpc.ManagedChannelBuilder

class GRPCClientBuilder(implicit S: Serializer, CS: ContextShift[IO]) extends RpcClientBuilder[IO] {
  override def build(node: Node): RpcClient[IO] = {

    val builder: ManagedChannelBuilder[_] = ManagedChannelBuilder
      .forAddress(node.host, node.port)
      .enableRetry()
      .usePlaintext()

    new GRPCClient(node, builder.build(), S)
  }
}
