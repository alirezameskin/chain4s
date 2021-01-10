package chain4s.rpc.member

trait RpcServer[F[_]] {
  def start: F[Unit]
  def stop: F[Unit]
}
