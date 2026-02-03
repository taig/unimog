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

import java.time.Duration
import java.time.Instant
import java.util.UUID

abstract class Unimog[F[_], A]:
  self =>

  def find(identifier: UUID, now: Instant): F[Option[Message[A]]]

  final def find(identifier: UUID)(using Monad[F], Clock[F]): F[Option[Message[A]]] =
    realTimeInstant.flatMap(find(identifier, _))

  def publish(messages: NonEmptyList[Message.Create[A]]): F[Unit]

  final def publish(messages: List[Message.Create[A]])(using F: Applicative[F]): F[Unit] =
    NonEmptyList.fromList(messages).fold(F.unit)(publish)

  final def publish1(message: Message.Create[A])(using Applicative[F]): F[Unit] = publish(messages = List(message))

  final def publish1(payload: A, lifespan: Duration = Duration.ofSeconds(30))(using
      Monad[F],
      Clock[F],
      UUIDGen[F]
  ): F[Message[A]] = for
    now <- realTimeInstant[F]
    identifier <- UUIDGen.randomUUID
    message = Message.Create(created = now, identifier, lifespan, payload)
    _ <- publish1(message)
  yield message.toMessage(status = Message.Status.Pending)

  def subscribeAck(chunk: Int): Stream[F, Acknowledgable[F, Message[A]]]

  final def subscribe[B](chunk: Int)(f: Message[A] => F[B])(using Apply[F]): Stream[F, B] =
    subscribeAck(chunk).evalMap(message => f(message.value) <* message.ack)

  final def mapK[G[_]](fK: [A] => F[A] => G[A]): Unimog[G, A] = new Unimog[G, A]:
    override def find(identifier: UUID, now: Instant): G[Option[Message[A]]] = fK(self.find(identifier, now))

    override def publish(messages: NonEmptyList[Message.Create[A]]): G[Unit] = fK(self.publish(messages))

    override def subscribeAck(chunk: Int): Stream[G, Acknowledgable[G, Message[A]]] =
      self.subscribeAck(chunk).map(_.mapK(fK)).translate(FunctionK.lift[F, G](fK))
