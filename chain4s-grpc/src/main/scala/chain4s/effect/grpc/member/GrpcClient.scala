package chain4s.effect.grpc.member

import cats.effect.{ContextShift, IO}
import chain4s.grpc.protos
import chain4s.grpc.serializer.Serializer
import chain4s.rpc.member.RpcClient
import chain4s._
import io.grpc.ManagedChannel

import java.util.concurrent.TimeUnit
import scala.concurrent.blocking

private[grpc] class GrpcClient(node: Node, channel: ManagedChannel, serializer: Serializer)(implicit
  CS: ContextShift[IO]
) extends RpcClient[IO] {

  val stub = protos.ChainReplicationGrpc.stub(channel)

  override def last: IO[Long] =
    IO.fromFuture(IO(stub.last(protos.LastStateRequest()))).map(_.index)

  override def send(entry: LogEntry): IO[Acknowledgment] =
    IO.fromFuture {
      IO(stub.append(protos.AppendEntryRequest(entry.index, serializer.encode(entry.request))))
    }.map(_ => Accepted(node, entry.index))

  override def commit(index: Long): IO[Unit] =
    IO.fromFuture {
      IO(stub.commit(protos.CommitRequest(index)))
    }.as(())

  override def write(request: WriteRequest): IO[Unit] =
    IO.fromFuture {
      IO(
        stub.write(
          protos.WriteRequest(
            Some(protos.SenderInfo(request.sender.host, request.sender.port)),
            request.requestId,
            serializer.encode(request.command)
          )
        )
      )
    }.as(())

  override def read(request: ReadRequest): IO[request.RESULT] =
    IO.fromFuture {
      IO(
        stub.read(protos.ReadRequest(serializer.encode(request.command)))
      )
    }.map { response =>
      serializer.decode[request.RESULT](response.result)
    }

  override def close: IO[Unit] =
    IO.delay {
      channel.shutdown()
      if (!blocking(channel.awaitTermination(30, TimeUnit.SECONDS))) {
        channel.shutdownNow()
        ()
      }
    }
}
