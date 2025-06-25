package io.taig.unimog.sql.schema

import cats.syntax.all.*
import io.taig.unimog.Message
import io.taig.unimog.sql.codecs.*
import skunk.Codec
import skunk.codec.all.*

import java.time.Instant
import java.util.UUID

final private[unimog] case class MessageSqlSchema(
    created: Instant,
    identifier: UUID,
    payload: String,
    pending: Option[Instant]
):
  def toMessage: Message = Message(created, identifier, payload)

private[unimog] object MessageSqlSchema:
  def apply(message: Message): MessageSqlSchema =
    MessageSqlSchema(
      created = message.created,
      identifier = message.identifier,
      payload = message.payload,
      pending = none
    )

  val codec: Codec[MessageSqlSchema] = (instant *: uuid *: text *: instant.opt).to
