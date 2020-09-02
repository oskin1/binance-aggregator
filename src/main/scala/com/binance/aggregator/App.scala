package com.binance.aggregator

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, Resource}
import cats.tagless.syntax.functorK._
import cats.tagless.syntax.invariantK._
import com.binance.aggregator.clients.{BinanceClient, Ws}
import com.binance.aggregator.context.AppContext
import com.binance.aggregator.db.{doobieLogging, PostgresXa}
import com.binance.aggregator.modules.Aggregator
import com.binance.aggregator.repositories.CandlestickRepo
import com.binance.aggregator.services.AggregationService
import doobie.implicits._
import fs2.{Chunk, Stream}
import monix.eval.{Task, TaskApp}
import sttp.client.asynchttpclient.fs2.{AsyncHttpClientFs2Backend, Fs2WebSocketHandler}
import tofu.WithRun
import tofu.doobie.instances.implicits._
import tofu.doobie.transactor.Txr
import tofu.env.Env
import tofu.fs2Instances._
import tofu.lift.Lift
import tofu.logging.{LoggableContext, Logs}
import tofu.syntax.monadic._

object App extends TaskApp {

  override def run(args: List[String]): Task[ExitCode] =
    init.use { case (program, ctx) => program.run.compile.drain.run(ctx) as ExitCode.Success }

  type InitF[+A]   = Task[A]
  type AppF[+A]    = Env[AppContext, A]
  type StreamF[+A] = Stream[AppF, A]

  implicit private val logs: Logs[InitF, AppF]                = Logs.withContext[InitF, AppF]
  implicit private val loggableContext: LoggableContext[AppF] = LoggableContext.of[AppF].instance[AppContext]

  private def init: Resource[InitF, (Aggregator[StreamF], AppContext)] =
    for {
      ctx                                         <- Resource.liftF(AppContext[InitF])
      xa                                          <- PostgresXa[InitF](ctx.conf.db)
      txr                                          = Txr.contextual[AppF](xa)
      elh                                         <- Resource.liftF(doobieLogging.makeEmbeddableHandler[InitF, AppF, txr.DB]("doobie"))
      implicit0(l: Logs[InitF, txr.DB])            = logs.mapK(Lift[AppF, txr.DB].liftF)
      repo                                        <- Resource.liftF(CandlestickRepo[InitF, txr.DB](elh))
      implicit0(repoF: CandlestickRepo[AppF])      = repo.mapK(txr.trans)
      implicit0(srv: AggregationService[StreamF]) <- Resource.liftF(AggregationService[StreamF, InitF, AppF, Chunk])
      wr                                           = implicitly[WithRun[AppF, InitF, AppContext]]
      implicit0(ws: Ws[AppF])                     <- Resource.liftF(makeWs[InitF].map(_.imapK(wr.liftF)(wr.runContextK(ctx))))
      implicit0(client: BinanceClient[StreamF])    = BinanceClient[StreamF, AppF]
      aggregator                                  <- Resource.liftF(Aggregator[StreamF, InitF, AppF])
    } yield aggregator -> ctx

  private def makeWs[F[_]: ConcurrentEffect: ContextShift]: F[Ws[F]] =
    for {
      implicit0(wsh: WsHandler[F])    <- Fs2WebSocketHandler[F]()
      implicit0(back: HttpBackend[F]) <- AsyncHttpClientFs2Backend[F]()
    } yield Ws[F]
}
