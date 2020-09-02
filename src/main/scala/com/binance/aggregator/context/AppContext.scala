package com.binance.aggregator.context

import cats.Applicative
import cats.syntax.functor._
import com.binance.aggregator.config.ConfigBundle
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@ClassyOptics
final case class AppContext(
  @promote conf: ConfigBundle,
  traceId: TraceId
)

object AppContext {

  def apply[F[_]: Applicative]: F[AppContext] =
    ConfigBundle[F] map (AppContext(_, TraceId("[Initial]")))

  implicit def loggable: Loggable[AppContext] = Loggable[TraceId].contramap[AppContext](_.traceId)
}
