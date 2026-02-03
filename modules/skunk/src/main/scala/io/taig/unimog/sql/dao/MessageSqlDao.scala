package io.taig.unimog.sql.dao

import cats.Functor
import cats.data.NonEmptyList
import cats.syntax.all.*
import fs2.Stream
import io.taig.unimog.Message
import io.taig.unimog.sql.query.MessageSqlQuery
import io.taig.unimog.sql.schema.MessageSqlSchema
import skunk.Codec
import skunk.Session
import skunk.Void
import skunk.data.Completion

import java.time.Instant
import java.util.UUID

final private[unimog] class MessageSqlDao[F[_]: Functor, A](message: MessageSqlQuery[A]):
  def create(messages: NonEmptyList[Message.Create[A]]): Session[F] => F[Completion] =
    _.execute[Void](message.insert(messages.map(MessageSqlSchema.apply)))(Void)

  def find(identifier: UUID, now: Instant): Session[F] => F[Option[Message[A]]] =
    _.option(message.selectByIdentifier)(identifier).map(_.map(_.toMessage(now)))

  def update(identifier: UUID, completed: Instant): Session[F] => F[Completion] =
    _.execute(message.updateCompletedByIdentifier)((completed, identifier))

  def list(limit: Int, now: Instant): Session[F] => Stream[F, Message[A]] =
    _.stream(message.select(limit))(now, chunkSize = 4096).map(_.toMessage(now))

private[unimog] object MessageSqlDao:
  def apply[F[_]: Functor, A](schema: String, payload: Codec[A]): MessageSqlDao[F, A] =
    new MessageSqlDao(message = MessageSqlQuery(payload, schema))
