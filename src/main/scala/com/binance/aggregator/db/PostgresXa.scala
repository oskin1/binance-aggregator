package com.binance.aggregator.db

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import com.binance.aggregator.config.PostgresConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object PostgresXa {

  def apply[F[_]: Async: ContextShift](
    config: PostgresConfig
  ): Resource[F, HikariTransactor[F]] =
    for {
      cp      <- ExecutionContexts.fixedThreadPool(16)
      blocker <- Blocker[F]
      xa      <- HikariTransactor.newHikariTransactor[F](
                   driverClassName = "org.postgresql.Driver",
                   config.url,
                   config.user,
                   config.pass,
                   cp,
                   blocker
                 )
      _       <- Resource.liftF(configure(xa))
    } yield xa

  private def configure[F[_]: Sync](
    xa: HikariTransactor[F]
  ): F[Unit] =
    xa.configure { c =>
      Sync[F].delay {
        c.setAutoCommit(false)
        c.setMaxLifetime(600000)
        c.setIdleTimeout(30000)
      }
    }
}
