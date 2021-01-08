package chain4s.rpc

import chain4s.{Acknowledgment, LogEntry}

trait RpcClient[F[_]] {
  def last(): F[Long]

  def send(entry: LogEntry): F[Acknowledgment]

  def commit(index: Long): F[Unit]

  def close(): F[Unit]
}
