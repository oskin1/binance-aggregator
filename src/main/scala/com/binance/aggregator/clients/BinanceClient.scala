package com.binance.aggregator.clients

import cats.FlatMap
import cats.effect.Concurrent
import cats.tagless.syntax.functorK._
import com.binance.aggregator.config.BinanceConfig
import com.binance.aggregator.context.HasBinanceConfig
import com.binance.aggregator.domain.Trade
import derevo.derive
import fs2.Stream
import fs2.concurrent.Queue
import tofu.data.derived.ContextEmbed
import tofu.fs2.LiftStream
import tofu.higherKind.derived.representableK
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._

@derive(representableK)
trait BinanceClient[S[_]] {

  def tradeStream: S[Trade]
}

object BinanceClient extends ContextEmbed[BinanceClient] {

  def apply[S[_]: FlatMap: LiftStream[*[_], F], F[_]: Concurrent: HasBinanceConfig: Ws]: BinanceClient[S] =
    Stream.eval(context map (conf => new Live[F](conf): BinanceClient[Stream[F, *]])).embed.mapK(LiftStream[S, F].liftF)

  final private class Live[F[_]: Concurrent: HasBinanceConfig](conf: BinanceConfig)(implicit ws: Ws[F])
    extends BinanceClient[Stream[F, *]] {

    def tradeStream: Stream[F, Trade] =
      Stream.force {
        Queue.unbounded[F, Trade] map { q =>
          val consume = ws.consumeFrom(conf.apiEndpointUri)(_.evalMap(Trade.parse[F]).evalMap(q.enqueue1))
          q.dequeue concurrently Stream.eval(consume)
        }
      }
  }
}
