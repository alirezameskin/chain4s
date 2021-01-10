package chain4s

trait ReplyReceiver[F[_]] {
  def receive(requestId: Long, result: Any): F[Unit]
}
