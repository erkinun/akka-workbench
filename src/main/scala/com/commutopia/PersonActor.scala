package com.commutopia

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}

case class PersonRequest(name: String)
case class PersonResponse(name: String, age: Int)
class PersonActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case PersonRequest(name) => sender() ! PersonResponse(name, 32)
  }
}

object PersonActor {
  def props = Props[PersonActor]
  def shardName = "Person"

  val extractShardId: ExtractShardId = {
    case PersonRequest(n) => (n.head.toInt % 2).toString
  }

  val extractEntityId: ExtractEntityId = {
    case msg @ PersonRequest(n) => (n, msg)
  }
}
