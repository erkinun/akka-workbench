

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
import scala.util.{Random, Success}
import scala.io.StdIn

case class Person(name: String, age: Int)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val personFormat = jsonFormat2(Person)
}

object WebServer extends JsonSupport {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val numbers = Source.fromIterator(() =>
      Iterator.continually(Random.nextInt()))

    val futureExec = Future {
      Random.nextInt()
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
      personResource()

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  private def futureResource(futureExec: Future[Int]): Route = {
    path("future") {
      get {
        onComplete(futureExec) {
          case Success(value) => complete(s"result is $value")
        }
      }
    }
  }

  private def personResource(): Route = {
    
    path("person") {
      get {
        complete {
          Person("erkin", 32)
        }
      }
    }
  }
}