package chain4s.rpc.member

import chain4s.{ChainReplication, Node}

trait RpcServerBuilder[F[_]] {
  def build(node: Node, raft: ChainReplication[F]): F[RpcServer[F]]
}

object RpcServerBuilder {
  def apply[F[_]](implicit builder: RpcServerBuilder[F]): RpcServerBuilder[F] = builder
}
