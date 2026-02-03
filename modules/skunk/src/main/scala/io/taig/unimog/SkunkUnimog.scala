package io.taig.unimog

import cats.data.Kleisli
import cats.effect.Resource
import cats.effect.Temporal
import skunk.Session

import scala.annotation.nowarn
import scala.concurrent.duration.*
import skunk.Codec

object SkunkUnimog:
  @nowarn("msg=unused")
  def apply[F[_]: Temporal, A](sessions: Resource[F, Session[F]])(
      payload: Codec[A],
      poll: FiniteDuration,
      schema: String
  ): Unimog[F, A] = SkunkTransactionalUnimog[F, A](payload, poll, schema)
    .mapK([A] => (fa: Kleisli[F, Session[F], A]) => sessions.use(fa.run))
