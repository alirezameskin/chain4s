package chain4s.rpc.reply

trait RpcReplyClient[F[_]] {
  def reply[T](requestId: Long, result: T): F[Unit]

  def stop: F[Unit]
}
