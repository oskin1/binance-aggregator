package com.binance.aggregator.domain

import cats.data.NonEmptyChain
import cats.syntax.foldable._
import cats.{Foldable, Show}
import tofu.logging.Loggable

final case class Candlestick(
  ticker: String,
  ts: Long,
  open: BigDecimal,
  close: BigDecimal,
  low: BigDecimal,
  high: BigDecimal,
  volume: Long
)

object Candlestick {

  implicit val show: Show[Candlestick] = _.toString

  implicit val loggable: Loggable[Candlestick] = Loggable.show

  def aggregate[C[_]: Foldable](ticker: String, trades: C[Trade]): Option[Candlestick] =
    NonEmptyChain.fromSeq(trades.toList) map { nonEmptyTrades =>
      val open   = nonEmptyTrades.head.price
      val close  = nonEmptyTrades.last.price
      val low    = nonEmptyTrades.toList.minBy(_.price).price
      val high   = nonEmptyTrades.toList.maxBy(_.price).price
      val volume = nonEmptyTrades.map(_.quantity).toList.sum
      val ts     = nonEmptyTrades.head.ts
      Candlestick(ticker, ts, open, high, low, close, volume)
    }
}
