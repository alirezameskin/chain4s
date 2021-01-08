package chain4s

import cats.Monad
import cats.implicits._
import chain4s.rpc.RpcServer

class Cluster[F[_]: Monad](replication: ChainReplication[F], rpcServer: RpcServer[F]) {

  def start: F[Unit] =
    for {
      _ <- rpcServer.start
      _ <- replication.start
    } yield ()

  def stop: F[Unit] =
    for {
      _ <- rpcServer.stop
      _ <- replication.stop
    } yield ()

  def executeWrite(request: WriteRequest): F[Unit] =
    replication.write(request)

  def executeRead(request: ReadRequest): F[request.RESULT] =
    replication.read(request)
}
