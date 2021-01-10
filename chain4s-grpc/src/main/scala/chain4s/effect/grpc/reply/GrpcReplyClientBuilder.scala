package chain4s.effect.grpc.reply

import cats.effect.{ContextShift, IO}
import chain4s.Address
import chain4s.grpc.serializer.Serializer
import chain4s.rpc.reply.{RpcReplyClient, RpcReplyClientBuilder}
import io.grpc.ManagedChannelBuilder

class GrpcReplyClientBuilder(implicit S: Serializer, CS: ContextShift[IO]) extends RpcReplyClientBuilder[IO] {
  override def build(address: Address): IO[RpcReplyClient[IO]] =
    IO {
      val builder: ManagedChannelBuilder[_] = ManagedChannelBuilder
        .forAddress(address.host, address.port)
        .enableRetry()
        .usePlaintext()

      new GrpcReplyClient(address, builder.build(), S)
    }
}
