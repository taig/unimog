package io.taig.unimog.sql.dao

import cats.data.NonEmptyList
import fs2.Stream
import io.taig.unimog.Message
import io.taig.unimog.sql.query.MessageSqlQuery
import io.taig.unimog.sql.schema.MessageSqlSchema
import skunk.Session
import skunk.Void
import skunk.data.Completion

import java.time.Instant
import java.util.UUID
import skunk.Codec

final private[unimog] class MessageSqlDao[F[_], A](message: MessageSqlQuery[A]):
  def create(messages: NonEmptyList[Message.Create[A]]): Session[F] => F[Completion] =
    _.execute[Void](message.insert(messages.map(MessageSqlSchema.apply)))(Void)

  def update(identifier: UUID, completed: Instant): Session[F] => F[Completion] =
    _.execute(message.updateCompletedByIdentifier)((completed, identifier))

  def list(limit: Int)(now: Instant): Session[F] => Stream[F, Message[A]] =
    _.stream(message.select(limit))(now, chunkSize = 4096).map(_.toMessage(now))

private[unimog] object MessageSqlDao:
  def apply[F[_], A](schema: String, payload: Codec[A]): MessageSqlDao[F, A] =
    new MessageSqlDao(message = MessageSqlQuery(payload, schema))
