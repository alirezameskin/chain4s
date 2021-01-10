package chain4s.demo

import cats.effect.IO
import cats.effect.concurrent.Ref
import chain4s.demo.command.{GetCommand, SetCommand}
import chain4s.{ReadCommand, StableLog, WriteCommand}

class StableLogImpl(itemsRef: Ref[IO, Map[String, String]]) extends StableLog[IO] {
  override def commit(command: WriteCommand): IO[command.RESULT] =
    command match {
      case req: SetCommand => set(req).map(_.asInstanceOf[command.RESULT])
      case req             => IO.raiseError(new RuntimeException(s"Invalid request ${req}"))
    }

  override def query(command: ReadCommand): IO[command.RESULT] =
    command match {
      case GetCommand(key) => get(key).map(_.asInstanceOf[command.RESULT])
      case req             => IO.raiseError(new RuntimeException(s"Invalid read request ${req}"))
    }

  private def set(req: SetCommand): IO[String] =
    itemsRef.updateAndGet(_ + (req.key -> req.value)).map(_ => req.value)

  private def get(key: String): IO[String] =
    for (items <- itemsRef.get) yield items(key)
}

object StableLogImpl {
  def empty(): IO[StableLog[IO]] =
    for {
      items <- Ref.of[IO, Map[String, String]](Map.empty)
    } yield new StableLogImpl(items)
}
