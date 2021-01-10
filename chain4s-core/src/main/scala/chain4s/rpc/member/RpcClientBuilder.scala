package chain4s.rpc.member

import chain4s.Node

trait RpcClientBuilder[F[_]] {
  def build(address: Node): F[RpcClient[F]]
}

object RpcClientBuilder {
  def apply[F[_]](implicit instance: RpcClientBuilder[F]): RpcClientBuilder[F] = instance
}
