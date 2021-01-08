package chain4s.demo

case class GetCommand(key: String) extends chain4s.ReadCommand {
  override type RESULT = String
}
