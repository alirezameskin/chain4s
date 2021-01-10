package chain4s.proxy

import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.{Concurrent, Resource}
import cats.implicits._
import cats.{Monad, MonadError}
import chain4s.rpc._
import chain4s._
import chain4s.rpc.member.{RpcClient, RpcClientBuilder}
import chain4s.rpc.reply.RpcReplyReceiverBuilder

class ReplicationProxy[F[_]: RpcClientBuilder: Concurrent](
  self: Address,
  headRef: Ref[F, RpcClient[F]],
  tailRef: Ref[F, RpcClient[F]],
  requestIdRef: Ref[F, Long],
  requestsRef: Ref[F, Map[Long, Deferred[F, Any]]]
) extends ClusterClient[F] {

  override def start: F[Unit] =
    Monad[F].unit

  override def close: F[Unit] =
    for {
      c <- headRef.get
      _ <- c.close
      c <- tailRef.get
      _ <- c.close
    } yield ()

  override def write(command: WriteCommand): F[command.RESULT] =
    for {
      requestId <- requestIdRef.updateAndGet(_ + 1)
      client    <- headRef.get
      deferred  <- Deferred[F, Any]
      _         <- requestsRef.update(items => items + (requestId -> deferred))
      _         <- client.write(WriteRequest(self, requestId, command))
      result    <- deferred.get
    } yield result.asInstanceOf[command.RESULT]

  override def read(command: ReadCommand): F[command.RESULT] =
    for {
      client <- tailRef.get
      result <- client.read(ReadRequest(command))
    } yield result.asInstanceOf[command.RESULT]

  def onReplyReceived(requestId: Long, result: Any): F[Unit] =
    for {
      requests <- requestsRef.get
      _        <- requests.get(requestId).map(_.complete(result)).getOrElse(Monad[F].unit)
      _        <- requestsRef.update(_ - requestId)
    } yield ()
}

object ReplicationProxy {
  def build[F[_]: Concurrent: RpcClientBuilder: RpcReplyReceiverBuilder](address: Address, config: ClusterConfiguration)(implicit
    ME: MonadError[F, Throwable]
  ): Resource[F, ReplicationProxy[F]] =
    config.nodes.toList match {
      case Nil =>
        Resource.liftF(ME.raiseError(new RuntimeException("Empty nodes list")))

      case head :: Nil =>
        for {
          client <- makeClient(address, head, head)
          _      <- makeReplyReceiver(address, client)
        } yield client

      case head :: tail =>
        for {
          client <- makeClient(address, head, tail.last)
          _      <- makeReplyReceiver(address, client)
        } yield client
    }

  private def makeReplyReceiver[F[_]: Concurrent: RpcReplyReceiverBuilder](address: Address, client: ReplicationProxy[F]) = {
    val receiver: ReplyReceiver[F] = (requestId: Long, result: Any) => client.onReplyReceived(requestId, result)
    val acquire = for {
      server <- RpcReplyReceiverBuilder[F].build(address, receiver)
      _      <- server.start
    } yield server

    Resource.make(acquire)(_.stop)
  }

  private def makeClient[F[_]: Concurrent: RpcClientBuilder](address: Address, head: Node, tail: Node) = {
    val acquire = for {
      headClient <- RpcClientBuilder[F].build(head)
      tailClient <- RpcClientBuilder[F].build(tail)
      headRef    <- Ref.of[F, RpcClient[F]](headClient)
      tailRef    <- Ref.of[F, RpcClient[F]](tailClient)
      requests   <- Ref.of[F, Map[Long, Deferred[F, Any]]](Map.empty)
      requestId  <- Ref.of[F, Long](0)
    } yield new ReplicationProxy[F](address, headRef, tailRef, requestId, requests)
    Resource.make(acquire)(_.close)
  }
}
