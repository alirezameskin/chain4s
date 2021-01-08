package chain4s.effect.grpc

import cats.effect.IO
import chain4s.grpc.protos
import chain4s.grpc.serializer.Serializer
import chain4s.rpc.{RpcServer, RpcServerBuilder}
import chain4s.{ChainReplication, Node}
import io.grpc.ServerBuilder

import java.util.concurrent.TimeUnit
import scala.concurrent.blocking

class GRPCServerBuilder(implicit S: Serializer) extends RpcServerBuilder[IO] {
  override def build(node: Node, raft: ChainReplication[IO]): IO[RpcServer[IO]] =
    IO.delay {
      val service = protos.ChainReplicationGrpc.bindService(
        new GRPCService(raft, S),
        scala.concurrent.ExecutionContext.global
      );

      val builder: ServerBuilder[_] = ServerBuilder
        .forPort(node.port)
        .addService(service)

      val server = builder.build()

      new RpcServer[IO] {
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
