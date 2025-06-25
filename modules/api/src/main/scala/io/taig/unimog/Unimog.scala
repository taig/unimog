package io.taig.unimog

import cats.Monad
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

abstract class Unimog[F[_]]:
  def publish(message: Message): F[Unit]

  final def publish(payload: String)(using clock: Clock[F], uuids: UUIDGen[F])(using Monad[F]): F[Message] = for
    now <- realTimeInstant[F]
    identifier <- uuids.randomUUID
    message = Message(created = now, identifier, payload)
    _ <- publish(message)
  yield message

  def subscribe(block: Int, stale: FiniteDuration): Stream[F, Acknowledgable[F, Message]]
