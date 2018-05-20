package com.commutopia

import java.nio.file.Paths

import akka.actor.ActorRef
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Random, Success}

class HTTPServer(personActor: ActorRef) extends JsonSupport  {

  implicit val ec = ExecutionContext.global
  
  val csvFile = Paths.get("files/results.csv")
  val fileSource: Source[ByteString, Future[IOResult]] = FileIO.fromPath(csvFile)
  val lineSource: Source[CsvLine, Future[IOResult]] =
    fileSource.via(Framing.delimiter(ByteString("\r\n"), maximumFrameLength = 100, allowTruncation = true))
      .map(_.utf8String)
      .map(line => CsvLine(line.split(", ").toList))

  val numbers = Source.fromIterator(() =>
    Iterator.continually(Random.nextInt()))

  val futureExec = Future {
    Random.nextInt()
  }

  def fileResource: Route = {

    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    path("file") {
      get {
        complete {
          HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            fileSource
          )
        }
      }
    } ~
      path ("line") {
        get {
          complete {
            lineSource
          }
        }
      }
  }

  def futureResource(futureExec: Future[Int])(implicit ec: ExecutionContext): Route = {
    path("future") {
      get {
        onComplete(futureExec) {
          case Success(value) => complete(s"result is $value")
        }
      }
    } ~
      path("futureDirect") {
        get {
          import akka.http.scaladsl.marshalling.GenericMarshallers.futureMarshaller
          complete(Future(Person("hede", 52)))
        }
      }
  }

  def personResource: Route = {
    path("person") {
      get {
        complete {
          import akka.http.scaladsl.marshalling.GenericMarshallers.futureMarshaller
          personActor.ask(PersonRequest("erkin"))(5 seconds).mapTo[PersonResponse].map(p => Person(p.name, p.age))
        }
      }
    }
  }

  val route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    } ~
    path("random") {
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            // transform each number to a chunk of bytes
            numbers.map(n => ByteString(s"$n\n"))
          )
        )
      }
    } ~
    futureResource(futureExec) ~
    personResource ~
    fileResource
}
