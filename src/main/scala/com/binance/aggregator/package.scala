package com.binance

import fs2.Stream
import sttp.client.SttpBackend
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.ws.WebSocket

package object aggregator {

  type HttpBackend[F[_]] = SttpBackend[F, Stream[F, Byte], WebSocketHandler]

  type WsHandler[F[_]] = WebSocketHandler[WebSocket[F]]
}
