package io.taig.unimog

import cats.Monad
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import fs2.Stream

import scala.concurrent.duration.FiniteDuration
import cats.Apply

abstract class Unimog[F[_]]:
  def publish(message: Message): F[Unit]

  final def publish(payload: String)(using clock: Clock[F], uuids: UUIDGen[F])(using Monad[F]): F[Message] = for
    now <- realTimeInstant[F]
    identifier <- uuids.randomUUID
    message = Message(created = now, identifier, payload)
    _ <- publish(message)
  yield message

  def subscribeAck(block: Int, stale: FiniteDuration): Stream[F, Acknowledgable[F, Message]]

  final def subscribe[A](block: Int, stale: FiniteDuration)(f: Message => F[A])(using Apply[F]): Stream[F, A] =
    subscribeAck(block, stale).evalMap(message => f(message.value) <* message.ack)
