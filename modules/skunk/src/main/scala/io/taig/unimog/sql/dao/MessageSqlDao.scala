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

private[unimog] object MessageSqlDao:
  def create[F[_]](messages: NonEmptyList[Message]): Session[F] => F[Completion] =
    _.execute[Void](MessageSqlQuery.insert(messages.map(MessageSqlSchema.apply)))(Void)

  def delete[F[_]](identifier: UUID): Session[F] => F[Completion] =
    _.execute(MessageSqlQuery.deleteByIdentifier)(identifier)

  def list[F[_]](limit: Int, stale: FiniteDuration)(now: Instant): Session[F] => Stream[F, Message] =
    _.stream(MessageSqlQuery.select(limit, stale))(now, chunkSize = 256).map(_.toMessage)
