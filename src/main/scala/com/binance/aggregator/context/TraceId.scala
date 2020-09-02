package com.binance.aggregator.context

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
final case class TraceId(value: String) extends AnyVal
