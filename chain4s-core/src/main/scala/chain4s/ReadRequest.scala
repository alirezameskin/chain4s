package chain4s

case class ReadRequest(command: ReadCommand) {
  type RESULT = command.type#RESULT
}
