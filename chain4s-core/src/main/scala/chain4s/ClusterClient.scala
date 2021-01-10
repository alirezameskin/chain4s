package chain4s

trait ClusterClient[F[_]] {

  def start: F[Unit]

  def close: F[Unit]

  def write(command: WriteCommand): F[command.RESULT]

  def read(command: ReadCommand): F[command.RESULT]
}
