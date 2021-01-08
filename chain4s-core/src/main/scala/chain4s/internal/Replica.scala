package chain4s.internal

import chain4s.Node

case class Replica(
  node: Node,
  predecessor: Option[Node],
  successor: Option[Node]
)
