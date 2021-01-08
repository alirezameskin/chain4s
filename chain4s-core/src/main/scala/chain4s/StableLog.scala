package chain4s

trait StableLog[F[_]] {

  def commit(command: WriteCommand): F[command.type#RESULT]

  def query(command: ReadCommand): F[command.type#RESULT]
}
