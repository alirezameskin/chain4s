package chain4s

import cats.Monad
import cats.implicits._
import chain4s.internal.Logger
import chain4s.rpc.member.RpcServer

class Cluster[F[_]: Monad: Logger](replication: ChainReplication[F], rpcServer: RpcServer[F]) {

  def start: F[Unit] =
    for {
      _ <- Logger[F].trace("Cluster is started")
      _ <- rpcServer.start
      _ <- Logger[F].trace("Rpc Server is started")
      _ <- replication.start
      _ <- Logger[F].trace("Replication member is started")
    } yield ()

  def stop: F[Unit] =
    for {
      _ <- Logger[F].trace("Cluster is stopping")
      _ <- rpcServer.stop
      _ <- Logger[F].trace("Rpc Server is stopped")
      _ <- replication.stop
      _ <- Logger[F].trace("Replication member is stopped")
    } yield ()

  def executeWrite(request: WriteRequest): F[Unit] =
    replication.write(request)

  def executeRead(request: ReadRequest): F[request.RESULT] =
    replication.read(request)
}
