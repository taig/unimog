package io.taig.unimog

import cats.Applicative
import cats.Apply
import cats.Monad
import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

abstract class Unimog[F[_], A]:
  self =>

  def publish(messages: NonEmptyList[Message[A]]): F[Unit]

  final def publish(messages: List[Message[A]])(using F: Applicative[F]): F[Unit] =
    NonEmptyList.fromList(messages).fold(F.unit)(publish)

  final def publish1(message: Message[A])(using Applicative[F]): F[Unit] = publish(messages = List(message))

  final def publish1(payload: A)(using Clock[F], UUIDGen[F])(using Monad[F]): F[Message[A]] = for
    now <- realTimeInstant[F]
    identifier <- UUIDGen.randomUUID
    message = Message(created = now, identifier, payload)
    _ <- publish1(message)
  yield message

  def subscribeAck(block: Int, stale: FiniteDuration): Stream[F, Acknowledgable[F, Message[A]]]

  final def subscribe[B](block: Int, stale: FiniteDuration)(f: Message[A] => F[B])(using Apply[F]): Stream[F, B] =
    subscribeAck(block, stale).evalMap(message => f(message.value) <* message.ack)

  final def mapK[G[_]](fK: [A] => F[A] => G[A]): Unimog[G, A] = new Unimog[G, A]:
    override def publish(messages: NonEmptyList[Message[A]]): G[Unit] = fK(self.publish(messages))

    override def subscribeAck(
        block: Int,
        stale: FiniteDuration
    ): Stream[G, Acknowledgable[G, Message[A]]] =
      self.subscribeAck(block, stale).map(_.mapK(fK)).translate(FunctionK.lift[F, G](fK))
