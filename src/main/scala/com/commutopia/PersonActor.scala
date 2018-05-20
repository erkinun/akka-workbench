package com.commutopia

import akka.actor.{Actor, ActorLogging}

case class PersonRequest(name: String)
case class PersonResponse(name: String, age: Int)
class PersonActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case PersonRequest(name) => sender() ! PersonResponse(name, 32)
  }
}
