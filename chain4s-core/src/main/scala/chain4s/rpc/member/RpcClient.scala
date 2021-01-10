package chain4s.rpc.member

import chain4s.{Acknowledgment, LogEntry, ReadRequest, WriteRequest}

trait RpcClient[F[_]] {
  def last: F[Long]

  def send(entry: LogEntry): F[Acknowledgment]

  def commit(index: Long): F[Unit]

  def write(request: WriteRequest): F[Unit]

  def read(request: ReadRequest): F[request.RESULT]

  def close: F[Unit]
}
