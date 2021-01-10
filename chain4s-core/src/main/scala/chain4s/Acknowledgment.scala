package chain4s

sealed trait Acknowledgment

case class Accepted(node: Node, index: Long)     extends Acknowledgment
case class Rejected(node: Node, lastIndex: Long) extends Acknowledgment
