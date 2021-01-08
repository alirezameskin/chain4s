package chain4s.demo

import cats.effect.{ExitCode, IO, IOApp, Resource}
import chain4s._
import chain4s.effect.grpc.{GRPCClientBuilder, GRPCServerBuilder}
import chain4s.effect.{ReplicationCluster, SpeculativeLogImpl}
import chain4s.grpc.serializer.JavaSerializer
import chain4s.internal.Logger
import io.odin.Level
import io.odin.formatter.Formatter

import scala.concurrent.duration._

object TestApp extends IOApp {

  implicit val logger: Logger[IO] = chain4s.effect.odinLogger(
    io.odin.consoleLogger(formatter = Formatter.colorful, minLevel = Level.Trace)
  )

  val node1 = Node("localhost", 9180)
  val node2 = Node("localhost", 9181)
  val node3 = Node("localhost", 9182)
  val nodes = List(node1, node2, node3)

  override def run(args: List[String]): IO[ExitCode] =
    makeClusters.use { case (cluster1, cluster2, cluster3) =>
      for {
        _ <- cluster1.start
        _ <- logger.info("Cluster1 is started")
        _ <- cluster2.start
        _ <- logger.info("Cluster2 is started")
        _ <- cluster3.start
        _ <- logger.info("Cluster3 is started")
        _ <- cluster1.executeWrite(WriteRequest(Address("localhost", 1020), "request-id", SetCommand("Key1", "Value1")))

        _ <- IO.sleep(5.seconds)

        res <- cluster1.executeRead(ReadRequest(GetCommand("Key1")))
        _   <- logger.info(s"Result after executing Get request : ${res}")

      } yield ExitCode.Success
    }

  private def makeClusters() =
    for {
      cluster1 <- makeCluster(Configuration(node1, nodes))
      cluster2 <- makeCluster(Configuration(node2, nodes))
      cluster3 <- makeCluster(Configuration(node3, nodes))
    } yield (cluster1, cluster2, cluster3)

  private def makeCluster(config: Configuration): Resource[IO, Cluster[IO]] = {

    implicit val serializer    = new JavaSerializer
    implicit val clientBuilder = new GRPCClientBuilder()
    implicit val serverBuilder = new GRPCServerBuilder()

    for {
      log       <- Resource.liftF(SpeculativeLogImpl.empty)
      stableLog <- Resource.liftF(StableLogImpl.empty())
      cluster   <- ReplicationCluster.resource(config, log, stableLog)
    } yield cluster

  }
}
