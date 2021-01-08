package chain4s

trait SpeculativeLog[F[_]] {

  def getCommitIndex: F[Long]

  def setCommitIndex(index: Long): F[Unit]

  def getLastIndex: F[Long]

  def setLastIndex(index: Long): F[Unit]

  def entriesAfter(index: Long): F[Iterator[LogEntry]]

  def create(request: WriteRequest): F[LogEntry]

  def append(logEntry: LogEntry): F[LogEntry]

  def getEntry(index: Long): F[LogEntry]

}
