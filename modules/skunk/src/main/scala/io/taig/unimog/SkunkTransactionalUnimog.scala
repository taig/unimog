package io.taig.unimog

import cats.data.Kleisli
import cats.data.NonEmptyList
import cats.effect.Temporal
import cats.syntax.all.*
import fs2.Stream
import io.taig.unimog.sql.dao.MessageSqlDao
import skunk.Session

import java.util.UUID
import scala.concurrent.duration.*
import skunk.Codec

final class SkunkTransactionalUnimog[F[_]: Temporal, A](message: MessageSqlDao[F, A])(poll: FiniteDuration)
    extends Unimog[Kleisli[F, Session[F], *], A]:
  override def publish(messages: NonEmptyList[Message[A]]): Kleisli[F, Session[F], Unit] =
    Kleisli(message.create(messages)(_).void)

  override def subscribeAck(
      block: Int,
      stale: FiniteDuration
  ): Stream[Kleisli[F, Session[F], *], Acknowledgable[Kleisli[F, Session[F], *], Message[A]]] = Stream
    .repeatEval(next(block, stale))
    .flatMap:
      case Nil      => Stream.sleep_[Kleisli[F, Session[F], *]](poll)
      case messages => Stream.emits(messages)
    .map(message => Acknowledgable(ack = ack(identifier = message.identifier), message))

  def ack(identifier: UUID): Kleisli[F, Session[F], Unit] =
    Kleisli(message.delete(identifier)(_).void)

  def next(block: Int, stale: FiniteDuration): Kleisli[F, Session[F], List[Message[A]]] = Kleisli: session =>
    realTimeInstant.flatMap: now =>
      message.list(limit = block, stale)(now)(session).compile.toList

object SkunkTransactionalUnimog:
  def apply[F[_]: Temporal, A](
      payload: Codec[A],
      poll: FiniteDuration,
      schema: String
  ): Unimog[Kleisli[F, Session[F], *], A] =
    new SkunkTransactionalUnimog[F, A](message = MessageSqlDao[F, A](schema, payload))(poll)
