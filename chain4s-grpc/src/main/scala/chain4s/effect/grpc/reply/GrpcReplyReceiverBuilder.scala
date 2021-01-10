package chain4s.effect.grpc.reply

import cats.effect.IO
import chain4s.grpc.protos
import chain4s.grpc.protos.{ReplyRequest, ReplyResponse}
import chain4s.grpc.serializer.Serializer
import chain4s.rpc.reply.{RpcReplyReceiver, RpcReplyReceiverBuilder}
import chain4s.{Address, ReplyReceiver}
import io.grpc.ServerBuilder

import java.util.concurrent.TimeUnit
import scala.concurrent.{blocking, Future}

class GrpcReplyReceiverBuilder()(implicit S: Serializer) extends RpcReplyReceiverBuilder[IO] {
  override def build(address: Address, receiver: ReplyReceiver[IO]): IO[RpcReplyReceiver[IO]] =
    IO.delay {
      val service = protos.ReplicationClientGrpc.bindService(
        new protos.ReplicationClientGrpc.ReplicationClient {
          override def reply(request: ReplyRequest): Future[ReplyResponse] =
            receiver.receive(request.requestId, S.decode(request.result)).map(_ => ReplyResponse()).unsafeToFuture()
        },
        scala.concurrent.ExecutionContext.global
      );

      val builder: ServerBuilder[_] = ServerBuilder
        .forPort(address.port)
        .addService(service)

      val server = builder.build()

      new RpcReplyReceiver[IO] {
        override def start: IO[Unit] =
          IO.delay(server.start()) *> IO.unit

        override def stop: IO[Unit] =
          IO.delay {
            server.shutdown()
            if (!blocking(server.awaitTermination(30, TimeUnit.SECONDS))) {
              server.shutdownNow()
              ()
            }
          }
      }
    }
}
