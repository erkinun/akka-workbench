package com.commutopia

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

import scala.io.StdIn

case class Person(name: String, age: Int)
case class CsvLine(words: List[String])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val personFormat = jsonFormat2(Person.apply)
  implicit val csvFormat = jsonFormat1(CsvLine)
}

object WebServer extends JsonSupport {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("sharding")
    val personActor = system.actorOf(Props[PersonActor])
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val server = new HTTPServer(personActor)
    val bindingFuture = Http().bindAndHandle(server.route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}