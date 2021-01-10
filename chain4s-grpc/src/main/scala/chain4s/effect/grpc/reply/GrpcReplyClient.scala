package chain4s.effect.grpc.reply

import cats.effect.{ContextShift, IO}
import chain4s.{Address, Node}
import chain4s.grpc.protos
import chain4s.grpc.serializer.Serializer
import chain4s.rpc.reply.RpcReplyClient
import io.grpc.ManagedChannel

import java.util.concurrent.TimeUnit
import scala.concurrent.blocking

class GrpcReplyClient(address: Address, channel: ManagedChannel, serializer: Serializer)(implicit CS: ContextShift[IO])
    extends RpcReplyClient[IO] {

  val stub = protos.ReplicationClientGrpc.stub(channel)

  override def reply[T](requestId: Long, result: T): IO[Unit] =
    IO.fromFuture(
      IO(stub.reply(protos.ReplyRequest(requestId, serializer.encode(result))))
    ).as(())

  override def stop: IO[Unit] =
    IO.delay {
      channel.shutdown()
      if (!blocking(channel.awaitTermination(30, TimeUnit.SECONDS))) {
        channel.shutdownNow()
        ()
      }
    }
}
