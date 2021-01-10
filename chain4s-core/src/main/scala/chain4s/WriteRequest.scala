package chain4s

case class WriteRequest(sender: Address, requestId: Long, command: WriteCommand) {
  type RESULT = command.type#RESULT
}
