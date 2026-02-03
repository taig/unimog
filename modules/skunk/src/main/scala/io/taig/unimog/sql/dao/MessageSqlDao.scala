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

final private[unimog] class MessageSqlDao(message: MessageSqlQuery):
  def create[F[_]](messages: NonEmptyList[Message]): Session[F] => F[Completion] =
    _.execute[Void](message.insert(messages.map(MessageSqlSchema.apply)))(Void)

  def delete[F[_]](identifier: UUID): Session[F] => F[Completion] =
    _.execute(message.deleteByIdentifier)(identifier)

  def list[F[_]](limit: Int, stale: FiniteDuration)(now: Instant): Session[F] => Stream[F, Message] =
    _.stream(message.select(limit, stale))(now, chunkSize = 4096).map(_.toMessage)

private[unimog] object MessageSqlDao:
  def apply(schema: String): MessageSqlDao = new MessageSqlDao(message = MessageSqlQuery(schema))
