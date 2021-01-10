package chain4s.rpc.reply

import chain4s.Address

trait RpcReplyClientBuilder[F[_]] {

  def build(address: Address): F[RpcReplyClient[F]]
}

object RpcReplyClientBuilder {
  def apply[F[_]](implicit instance: RpcReplyClient[F]): RpcReplyClient[F] = instance
}
