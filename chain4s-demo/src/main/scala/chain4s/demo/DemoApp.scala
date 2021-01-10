package chain4s.demo

import cats.Show
import cats.effect.Console.io._
import cats.effect.{ExitCode, IO, IOApp}
import chain4s._
import chain4s.demo.command.{GetCommand, SetCommand}
import chain4s.internal.Logger
import fansi.Str
import io.odin.Level
import io.odin.formatter.Formatter

object DemoApp extends IOApp {

  implicit val fansiStrShow: Show[fansi.Str] = (t: Str) => t.render

  implicit val logger: Logger[IO] = chain4s.effect.odinLogger(
    io.odin.consoleLogger(formatter = Formatter.colorful, minLevel = Level.Trace)
  )

  val clusterConfiguration = ClusterConfiguration(
    List(
      Node(Address("localhost", 9180)),
      Node(Address("localhost", 9181)),
      Node(Address("localhost", 9182))
    )
  )

  val proxyNode = Address("localhost", 9173)

  override def run(args: List[String]): IO[ExitCode] =
    makeResource().use { case (servers, proxy) =>
      for {
        _   <- proxy.start
        _   <- servers.start
        res <- proxy.write(SetCommand("Key1", "Value1"))
        _   <- putStrLn(fansi.Color.Blue(s"\nSet Command is executed, result :${res}"))

        res <- proxy.read(GetCommand("Key1"))
        _   <- putStrLn(fansi.Color.Blue(s"\nResult after executing Get request : ${res}\n"))

      } yield ExitCode.Success

    }

  private def makeResource() =
    for {
      servers <- ReplicationServers.build(clusterConfiguration)
      proxy   <- ProxyServer.build(proxyNode, clusterConfiguration)
    } yield (servers, proxy)

}
