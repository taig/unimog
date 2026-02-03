package io.taig.unimog.sql.schema

import cats.syntax.all.*
import io.taig.unimog.Message
import io.taig.unimog.sql.codecs.*
import skunk.Codec
import skunk.codec.all.*

import java.time.Instant
import java.util.UUID

final private[unimog] case class MessageSqlSchema[A](
    created: Instant,
    identifier: UUID,
    payload: A,
    pending: Option[Instant]
):
  def toMessage: Message[A] = Message(created, identifier, payload)

private[unimog] object MessageSqlSchema:
  def apply[A](message: Message[A]): MessageSqlSchema[A] =
    MessageSqlSchema(
      created = message.created,
      identifier = message.identifier,
      payload = message.payload,
      pending = none
    )

  def codec[A](payload: Codec[A]): Codec[MessageSqlSchema[A]] = (instant *: uuid *: payload *: instant.opt).to
