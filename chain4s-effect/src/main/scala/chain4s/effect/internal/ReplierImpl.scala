package chain4s.effect.internal

import cats.effect.IO
import chain4s.WriteRequest
import chain4s.internal.{Logger, Replier}

class ReplierImpl(implicit logger: Logger[IO]) extends Replier[IO] {

  override def sendReply(request: WriteRequest)(result: request.RESULT): IO[Unit] =
    logger.trace(s"Sending Reply for ${request} result is ${result}")
}
