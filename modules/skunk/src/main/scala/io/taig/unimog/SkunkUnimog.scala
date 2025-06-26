package io.taig.unimog

import cats.effect.MonadCancelThrow
import cats.effect.Resource
import cats.effect.Temporal
import cats.effect.kernel.Clock
import cats.syntax.all.*
import fs2.Stream
import io.taig.unimog.sql.dao.MessageSqlDao
import skunk.Session

import java.util.UUID
import scala.concurrent.duration.*

final class SkunkUnimog[F[_]: MonadCancelThrow: Temporal](sessions: Resource[F, Session[F]])(poll: FiniteDuration)(using
    Clock[F]
) extends Unimog[F]:
  override def publish(message: Message): F[Unit] = sessions.use(MessageSqlDao.create(message)).void

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
