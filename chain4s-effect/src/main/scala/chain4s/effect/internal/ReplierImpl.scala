package chain4s.effect.internal

import cats.effect.IO
import chain4s.WriteRequest
import chain4s.internal.{Logger, Replier}
import chain4s.rpc.reply.RpcReplyClientBuilder

class ReplierImpl(builder: RpcReplyClientBuilder[IO])(implicit logger: Logger[IO]) extends Replier[IO] {

  override def sendReply(request: WriteRequest)(result: request.RESULT): IO[Unit] =
    for {
      _      <- logger.trace(s"Sending Result to ${request.sender}")
      client <- builder.build(request.sender)
      _      <- client.reply(request.requestId, result)
    } yield ()
}

object ReplierImpl {
  def build(implicit builder: RpcReplyClientBuilder[IO], logger: Logger[IO]) =
    new ReplierImpl(builder)
}
