package chain4s.rpc.reply

import chain4s.{Address, ReplyReceiver}

trait RpcReplyReceiverBuilder[F[_]] {
  def build(address: Address, receiver: ReplyReceiver[F]): F[RpcReplyReceiver[F]]
}

object RpcReplyReceiverBuilder {
  def apply[F[_]](implicit instance: RpcReplyReceiverBuilder[F]): RpcReplyReceiverBuilder[F] = instance
}
