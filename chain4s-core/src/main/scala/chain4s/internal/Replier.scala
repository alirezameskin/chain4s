package chain4s.internal

import chain4s.WriteRequest

trait Replier[F[_]] {
  def sendReply(request: WriteRequest)(result: request.RESULT): F[Unit]
}
