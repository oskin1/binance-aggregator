package com.binance.aggregator

import com.binance.aggregator.config.{AggregationConfig, BinanceConfig, ConfigBundle}
import tofu.{Context, HasContext}
import tofu.optics.Contains

package object context {

  type HasBinanceConfig[F[_]]     = F HasContext BinanceConfig
  type HasAggregationConfig[F[_]] = F HasContext AggregationConfig
  type HasConfig[F[_]]            = F HasContext ConfigBundle

  implicit def extractContext[F[_]: HasContext[*[_], AppContext], A](implicit
    lens: AppContext Contains A
  ): F HasContext A = Context[F].extract(lens)
}
