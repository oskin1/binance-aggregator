package com.binance.aggregator.config

import cats.Applicative
import cats.syntax.applicative._
import cats.instances.string._
import cats.data.NonEmptySet
import tofu.optics.macros.{promote, ClassyOptics}

import scala.concurrent.duration._

import scala.concurrent.duration.FiniteDuration

final case class BinanceConfig(
  apiEndpointUri: String
)

final case class AggregationConfig(
  epochLength: FiniteDuration,
  aggregateTickers: NonEmptySet[String]
)

final case class PostgresConfig(
  url: String,
  user: String,
  pass: String
)

@ClassyOptics
final case class ConfigBundle(
  @promote binance: BinanceConfig,
  @promote aggregation: AggregationConfig,
  db: PostgresConfig
)

object ConfigBundle {

  def apply[F[_]: Applicative]: F[ConfigBundle] =
    ConfigBundle(
      BinanceConfig(""),
      AggregationConfig(1.minute, NonEmptySet.of("BNBBTC")),
      PostgresConfig("", "", "")
    ).pure
}
