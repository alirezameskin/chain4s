package chain4s.effect

import cats.effect.IO
import cats.effect.concurrent.Ref
import chain4s.{LogEntry, SpeculativeLog, WriteRequest}

import scala.collection.SortedMap

class SpeculativeLogImpl(itemsRef: Ref[IO, SortedMap[Long, LogEntry]], lastIndexRef: Ref[IO, Long], commitIndexRef: Ref[IO, Long])
    extends SpeculativeLog[IO] {

  override def create(request: WriteRequest): IO[LogEntry] =
    for {
      index <- lastIndexRef.updateAndGet(_ + 1)
      entry = LogEntry(index, request)
      _ <- itemsRef.update(_ + (index -> entry))
      _ <- lastIndexRef.update(i => Math.max(i, index))
    } yield entry

  override def append(entry: LogEntry): IO[LogEntry] =
    for {
      _ <- itemsRef.update(_ + (entry.index -> entry))
      _ <- lastIndexRef.update(i => Math.max(i, entry.index))
    } yield entry

  override def getEntry(index: Long): IO[LogEntry] =
    itemsRef.get.map(_.get(index).orNull)

  override def entriesAfter(index: Long): IO[Iterator[LogEntry]] =
    for {
      items <- itemsRef.get
    } yield items.iterator.filter(_._1 > index).map(_._2)

  override def getCommitIndex: IO[Long] =
    commitIndexRef.get

  override def setCommitIndex(index: Long): IO[Unit] =
    commitIndexRef.set(index)

  override def getLastIndex: IO[Long] =
    lastIndexRef.get

  override def setLastIndex(index: Long): IO[Unit] =
    lastIndexRef.set(index)
}
object SpeculativeLogImpl {
  def empty: IO[SpeculativeLogImpl] =
    for {
      items  <- Ref.of[IO, SortedMap[Long, LogEntry]](SortedMap.empty)
      last   <- Ref.of[IO, Long](0L)
      commit <- Ref.of[IO, Long](0L)
    } yield new SpeculativeLogImpl(items, last, commit)
}
