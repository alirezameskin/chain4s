package chain4s

case class WriteRequest(sender: Address, requestId: String, command: WriteCommand) {
  type RESULT = command.type#RESULT
}
