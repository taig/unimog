package io.taig.unimog

import cats.data.NonEmptyList
import cats.effect.Resource
import cats.effect.Temporal
import cats.syntax.all.*
import fs2.Stream
import io.taig.unimog.sql.dao.MessageSqlDao
import skunk.Session

import java.util.UUID
import scala.concurrent.duration.*

final class SkunkUnimog[F[_]: Temporal](sessions: Resource[F, Session[F]])(
    poll: FiniteDuration
) extends Unimog[F]:
  override def publish(messages: NonEmptyList[Message]): F[Unit] = sessions.use(MessageSqlDao.create(messages)).void

  override def subscribeAck(block: Int, stale: FiniteDuration): Stream[F, Acknowledgable[F, Message]] = Stream
    .repeatEval(next(block, stale))
    .flatMap:
      case Nil      => Stream.sleep_(poll)
      case messages => Stream.emits(messages)
    .map(message => Acknowledgable(ack = ack(identifier = message.identifier), message))

  def ack(identifier: UUID): F[Unit] = sessions.use(MessageSqlDao.delete(identifier)).void

  def next(block: Int, stale: FiniteDuration): F[List[Message]] = sessions.use: sx =>
    realTimeInstant.flatMap: now =>
      MessageSqlDao.list(limit = block, stale)(now)(sx).compile.toList

object SkunkUnimog:
  def apply[F[_]: Temporal](sessions: Resource[F, Session[F]])(
      poll: FiniteDuration
  ): Unimog[F] =
    new SkunkUnimog[F](sessions)(poll)
