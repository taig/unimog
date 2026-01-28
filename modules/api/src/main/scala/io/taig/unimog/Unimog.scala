package io.taig.unimog

import cats.Applicative
import cats.Apply
import cats.Monad
import cats.data.NonEmptyList
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

abstract class Unimog[F[_]]:
  def publish(messages: NonEmptyList[Message]): F[Unit]

  final def publish(messages: List[Message])(using F: Applicative[F]): F[Unit] =
    NonEmptyList.fromList(messages).fold(F.unit)(publish)

  final def publish1(message: Message)(using Applicative[F]): F[Unit] = publish(messages = List(message))

  final def publish1(payload: String)(using Clock[F], UUIDGen[F])(using Monad[F]): F[Message] = for
    now <- realTimeInstant[F]
    identifier <- UUIDGen.randomUUID
    message = Message(created = now, identifier, payload)
    _ <- publish1(message)
  yield message

  def subscribeAck(block: Int, stale: FiniteDuration): Stream[F, Acknowledgable[F, Message]]

  final def subscribe[A](block: Int, stale: FiniteDuration)(f: Message => F[A])(using Apply[F]): Stream[F, A] =
    subscribeAck(block, stale).evalMap(message => f(message.value) <* message.ack)
