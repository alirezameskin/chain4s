package chain4s.rpc

trait RpcServer[F[_]] {
  def start: F[Unit]
  def stop: F[Unit]
}
