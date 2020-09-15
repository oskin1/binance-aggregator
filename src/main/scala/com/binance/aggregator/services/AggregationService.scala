package com.binance.aggregator.services

import cats.{FlatMap, Foldable, Functor, FunctorFilter, Monad}
import cats.syntax.functorFilter._
import com.binance.aggregator.config.AggregationConfig
import com.binance.aggregator.context.HasAggregationConfig
import com.binance.aggregator.domain.{Candlestick, Trade}
import com.binance.aggregator.repositories.CandlestickRepo
import tofu.data.derived.ContextEmbed
import tofu.higherKind.Embed
import tofu.logging.{Logging, Logs}
import tofu.syntax.streams.evals._
import tofu.syntax.streams.temporal._
import tofu.syntax.streams.filter._
import tofu.streams.{Evals, Temporal}
import tofu.fs2Instances._
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.logging._

trait AggregationService[S[_]] {

  def aggregate(ticker: String): S[Trade] => S[Candlestick]
}

object AggregationService extends ContextEmbed[AggregationService] {

  final private class ServiceContainer[F[_]: FlatMap](serviceF: F[AggregationService[F]])
    extends AggregationService[F] {
    def aggregate(ticker: String): F[Trade] => F[Candlestick] =
      in => serviceF >>= (_.aggregate(ticker)(in))
  }

  implicit val embed: Embed[AggregationService] =
    new Embed[AggregationService] {
      override def embed[F[_]](ft: F[AggregationService[F]])(implicit ev: FlatMap[F]): AggregationService[F] =
        new ServiceContainer[F](ft)
    }

  def apply[
    S[_]: Monad: Temporal[*[_], C]: Evals[*[_], F]: FunctorFilter: HasAggregationConfig,
    I[_]: Functor,
    F[_]: Functor,
    C[_]: Foldable
  ](implicit logs: Logs[I, F], repo: CandlestickRepo[F]): I[AggregationService[S]] =
    logs.forService[AggregationService[S]] map { implicit l =>
      (context[S] map (conf => new Live[S, F, C](conf): AggregationService[S])).embed
    }

  final private class Live[
    S[_]: Monad: Temporal[*[_], C]: Evals[*[_], F]: FunctorFilter,
    F[_]: Functor: Logging,
    C[_]: Foldable
  ](conf: AggregationConfig)(implicit repo: CandlestickRepo[F])
    extends AggregationService[S] {

    def aggregate(ticker: String): S[Trade] => S[Candlestick] =
      _.filter(_.symbol == ticker)
        .groupWithin(Int.MaxValue, conf.epochLength)
        .map(Candlestick.aggregate[C](ticker, _))
        .unNone
        .evalTap(c => info"Processing epoch ${c.ts} for ticker '${c.ticker}'")
        .evalTap(repo.insert)
  }
}
