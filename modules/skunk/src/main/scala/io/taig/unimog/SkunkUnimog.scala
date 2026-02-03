package io.taig.unimog

import cats.data.Kleisli
import cats.effect.Resource
import cats.effect.Temporal
import skunk.Session

import scala.annotation.nowarn
import scala.concurrent.duration.*

object SkunkUnimog:
  @nowarn("msg=unused")
  def apply[F[_]: Temporal](sessions: Resource[F, Session[F]])(
      poll: FiniteDuration,
      schema: String
  ): Unimog[F] = SkunkTransactionalUnimog[F](poll, schema)
    .mapK([A] => (fa: Kleisli[F, Session[F], A]) => sessions.use(fa.run))
