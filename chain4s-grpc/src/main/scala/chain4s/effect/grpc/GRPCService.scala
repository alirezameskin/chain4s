package chain4s.effect.grpc

import cats.effect.IO
import chain4s.grpc.protos
import chain4s.grpc.serializer.Serializer
import chain4s.{Address, ChainReplication, LogEntry, ReadRequest, WriteRequest}

import scala.concurrent.Future

private[grpc] class GRPCService(replication: ChainReplication[IO], serializer: Serializer)
    extends protos.ChainReplicationGrpc.ChainReplication {

  override def last(request: protos.LastStateRequest): Future[protos.LastStateResponse] =
    replication.speculativeLog.getLastIndex
      .map(protos.LastStateResponse(_))
      .unsafeToFuture()

  override def append(request: protos.AppendEntryRequest): Future[protos.AppendEntryResponse] =
    replication
      .onAppend(LogEntry(request.index, serializer.decode[WriteRequest](request.request)))
      .map(_ => protos.AppendEntryResponse())
      .unsafeToFuture()

  override def commit(request: protos.CommitRequest): Future[protos.CommitResponse] =
    replication
      .onCommit(request.index)
      .map(_ => protos.CommitResponse())
      .unsafeToFuture()

  override def write(request: protos.WriteRequest): Future[protos.WriteResponse] =
    replication
      .write(
        WriteRequest(
          Address(request.sender.get.host, request.sender.get.port),
          request.requestId,
          serializer.decode(request.command)
        )
      )
      .map(_ => protos.WriteResponse())
      .unsafeToFuture()

  override def read(request: protos.ReadRequest): Future[protos.ReadResponse] =
    replication
      .read(ReadRequest(serializer.decode(request.command)))
      .map(res => serializer.encode(res))
      .map(bytes => protos.ReadResponse(bytes))
      .unsafeToFuture()
}
