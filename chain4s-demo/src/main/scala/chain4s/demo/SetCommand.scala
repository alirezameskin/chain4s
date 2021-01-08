package chain4s.demo

import chain4s.WriteCommand

case class SetCommand(key: String, value: String) extends WriteCommand {
  override type RESULT = String
}
