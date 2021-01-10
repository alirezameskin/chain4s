package chain4s.rpc.reply

trait RpcReplyReceiver[F[_]] {

  def start: F[Unit]

  def stop: F[Unit]
}
