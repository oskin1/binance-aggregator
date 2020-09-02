package com.binance.aggregator.repositories

import cats.{FlatMap, Functor}
import com.binance.aggregator.domain.Candlestick
import doobie.ConnectionIO
import doobie._
import cats.tagless.syntax.functorK._
import derevo.derive
import tofu.data.derived.ContextEmbed
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.derived.representableK
import tofu.logging.Logs
import tofu.syntax.monadic._

@derive(representableK)
trait CandlestickRepo[D[_]] {

  def insert(item: Candlestick): D[Unit]
}

object CandlestickRepo extends ContextEmbed[CandlestickRepo] {

  def apply[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](
    elh: EmbeddableLogHandler[D]
  )(implicit logs: Logs[I, D]): I[CandlestickRepo[D]] =
    logs.forService[CandlestickRepo[D]] map { implicit l =>
      elh.embed(implicit lh => new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final private class Live(implicit lh: LogHandler) extends CandlestickRepo[ConnectionIO] {

    private val tableName = "candlestick"
    private val fields    = List("ticker", "ts", "open", "close", "low", "high", "volume")

    override def insert(item: Candlestick): ConnectionIO[Unit] =
      Update[Candlestick](
        s"insert into $tableName (${fields.mkString(", ")}) values (${fields.map(_ => "?").mkString(", ")})"
      ).run(item).void
  }
}
