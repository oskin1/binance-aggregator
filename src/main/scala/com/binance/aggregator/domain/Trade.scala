package com.binance.aggregator.domain

import cats.Applicative
import io.circe.Decoder
import tofu.Throws
import tofu.syntax.raise._

final case class Trade(ts: Long, symbol: String, price: BigDecimal, quantity: Long)

object Trade {

  implicit val decoder: Decoder[Trade] = { c =>
    for {
      ts     <- c.downField("T").as[Long]
      symbol <- c.downField("s").as[String]
      price  <- c.downField("p").as[String].map(BigDecimal(_))
      qty    <- c.downField("q").as[Long]
    } yield Trade(ts, symbol, price, qty)
  }

  def parse[F[_]: Applicative: Throws](raw: String): F[Trade] =
    io.circe.parser.parse(raw).flatMap(_.as[Trade]).toRaise[F]
}
