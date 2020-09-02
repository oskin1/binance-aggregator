package com.binance.aggregator.modules

import cats.{Functor, Monad}
import com.binance.aggregator.clients.BinanceClient
import com.binance.aggregator.config.AggregationConfig
import com.binance.aggregator.context.HasAggregationConfig
import com.binance.aggregator.services.AggregationService
import derevo.derive
import tofu.higherKind.derived.embed
import tofu.logging.{Logging, Logs}
import tofu.streams.{Broadcast, Evals}
import tofu.streams.syntax.broadcast._
import tofu.streams.syntax.evals._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.context._
import tofu.syntax.embed._

@derive(embed)
trait Aggregator[S[_]] {

  def run: S[Unit]
}

object Aggregator {

  def apply[
    S[_]: Monad: Broadcast: Evals[*[_], F]: HasAggregationConfig,
    I[_]: Functor,
    F[_]
  ](implicit
    logs: Logs[I, F],
    service: AggregationService[S],
    client: BinanceClient[S]
  ): I[Aggregator[S]] =
    logs.forService[Aggregator[S]] map { implicit l =>
      (context[S] map (conf => new Live(conf): Aggregator[S])).embed
    }

  final private class Live[S[_]: Broadcast: Evals[*[_], F], F[_]: Logging](
    conf: AggregationConfig
  )(implicit
    service: AggregationService[S],
    client: BinanceClient[S]
  ) extends Aggregator[S] {

    def run: S[Unit] =
      client.tradeStream
        .broadcastThrough(conf.aggregateTickers.toNonEmptyList.toList.map(service.aggregate): _*)
        .evalMap(cs => info"New candlestick $cs")
  }
}
