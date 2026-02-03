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
import scala.concurrent.duration.FiniteDuration
import skunk.Codec

final private[unimog] class MessageSqlDao[F[_], A](message: MessageSqlQuery[A]):
  def create(messages: NonEmptyList[Message[A]]): Session[F] => F[Completion] =
    _.execute[Void](message.insert(messages.map(MessageSqlSchema.apply)))(Void)

  def delete(identifier: UUID): Session[F] => F[Completion] =
    _.execute(message.deleteByIdentifier)(identifier)

  def list(limit: Int, stale: FiniteDuration)(now: Instant): Session[F] => Stream[F, Message[A]] =
    _.stream(message.select(limit, stale))(now, chunkSize = 4096).map(_.toMessage)

private[unimog] object MessageSqlDao:
  def apply[F[_], A](schema: String, payload: Codec[A]): MessageSqlDao[F, A] =
    new MessageSqlDao(message = MessageSqlQuery(payload, schema))
