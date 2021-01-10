package chain4s.demo.command

case class GetCommand(key: String) extends chain4s.ReadCommand {
  override type RESULT = String
}
