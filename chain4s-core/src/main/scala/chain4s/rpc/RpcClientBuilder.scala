package chain4s.rpc

import chain4s.Node

trait RpcClientBuilder[F[_]] {
  def build(address: Node): RpcClient[F]
}
