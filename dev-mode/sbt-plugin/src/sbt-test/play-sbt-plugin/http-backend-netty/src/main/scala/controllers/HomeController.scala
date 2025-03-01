/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import akka.stream.Materializer
import akka.stream.scaladsl.{ BroadcastHub, Flow, Keep, MergeHub, Sink, Source }

import javax.inject._
import play.api.http.websocket.{ CloseMessage, Message }
import play.api.mvc._
@Singleton
class HomeController @Inject()(cc: ControllerComponents)(implicit mat: Materializer) extends AbstractController(cc) {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("Successful response.")
  }

  // like a chat room: many clients -> merge hub -> broadcasthub -> many clients
  // makes it easy to make two websockets communicate with each other
  private val (chatSink, chatSource) = {
    // Don't log MergeHub$ProducerFailed as error if the client disconnects.
    // recoverWithRetries -1 is essentially "recoverWith"
    val source = MergeHub.source[String]
      .log("source") // See logback.xml (-> logger "akka.stream.Materializer")
      .recoverWithRetries(-1, { case _: Exception => Source.empty })

    val sink = BroadcastHub.sink[String]

    source.toMat(sink)(Keep.both).run()
  }

  // WebSocket that sends out messages that have been put into chatSink
  def websocketFeedback: WebSocket = WebSocket.accept[String, String](rh => Flow.fromSinkAndSource(Sink.ignore, chatSource))

  def websocket: WebSocket = WebSocket.accept[Message, Message](rh =>
    Flow.fromSinkAndSource(Sink.foreach(_ match {
      // When the client closes this WebSocket, send the status code
      // that we received from the client to the feedback-websocket
      case CloseMessage(statusCode, _) => Source.single(statusCode.map(_.toString).getOrElse("")).runWith(chatSink)
      case _ =>
    }), Source.maybe[Message]) // Keep connection open
  )
}
