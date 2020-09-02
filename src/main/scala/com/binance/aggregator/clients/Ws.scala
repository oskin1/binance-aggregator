package com.binance.aggregator.clients

import cats.effect.ConcurrentEffect
import cats.syntax.either._
import cats.tagless.InvariantK
import cats.~>
import com.binance.aggregator.{HttpBackend, WsHandler}
import fs2.{Pipe, Stream}
import sttp.client._
import sttp.client.impl.fs2._
import sttp.model.ws.WebSocketFrame
import tofu.syntax.monadic._

trait Ws[F[_]] {
  def consumeFrom(uri: String)(consumer: Pipe[F, String, Unit]): F[Unit]
}

object Ws {

  implicit val invK: InvariantK[Ws] =
    new InvariantK[Ws] {
      def imapK[F[_], G[_]](af: Ws[F])(fk: F ~> G)(gK: G ~> F): Ws[G] =
        new Ws[G] {
          def consumeFrom(uri: String)(consumer: Pipe[G, String, Unit]): G[Unit] =
            fk(af.consumeFrom(uri)(in => consumer(in.translate(fk)).translate(gK)))
        }
    }

  def apply[F[_]: ConcurrentEffect](implicit backend: HttpBackend[F], ws: WsHandler[F]): Ws[F] =
    new Ws[F] {

      def consumeFrom(uri: String)(consumer: Pipe[F, String, Unit]): F[Unit] =
        basicRequest
          .get(sttp.model.Uri(uri))
          .openWebsocket(ws)
          .flatMap { resp =>
            Fs2WebSockets.handleSocketThroughTextPipe(resp.result) { in =>
              Stream(WebSocketFrame.close.asLeft) merge in.through(consumer).drain
            }
          }
    }
}
