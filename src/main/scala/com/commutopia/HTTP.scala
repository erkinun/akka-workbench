package com.commutopia

import java.nio.file.Paths

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString

import scala.concurrent.Future

object HTTP extends JsonSupport  {

  val csvFile = Paths.get("files/results.csv")
  val fileSource: Source[ByteString, Future[IOResult]] = FileIO.fromPath(csvFile)
  val lineSource: Source[CsvLine, Future[IOResult]] =
    fileSource.via(Framing.delimiter(ByteString("\r\n"), maximumFrameLength = 100, allowTruncation = true))
      .map(_.utf8String)
      .map(line => CsvLine(line.split(", ").toList))

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
}
