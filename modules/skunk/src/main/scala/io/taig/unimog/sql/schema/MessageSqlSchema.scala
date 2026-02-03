package io.taig.unimog.sql.schema

import cats.syntax.all.*
import io.taig.unimog.Message
import io.taig.unimog.sql.codecs.*
import skunk.Codec
import skunk.codec.all.*

import java.time.Duration
import java.time.Instant
import java.util.UUID

final private[unimog] case class MessageSqlSchema[A](
    completed: Option[Instant],
    created: Instant,
    identifier: UUID,
    lifespan: Duration,
    payload: A,
    started: Option[Instant]
):
  def toMessageStatus(now: Instant): Message.Status = (started, completed) match
    case (Some(started), Some(completed)) =>
      Message.Status.Completed(started, finished = completed)
    case (Some(started), None) =>
      val expiration = started.plus(lifespan)
      if now.isAfter(expiration)
      then Message.Status.Rescheduled(expired = expiration)
      else Message.Status.Processing(started)
    case _ => Message.Status.Pending

  def toMessage(now: Instant): Message[A] =
    Message(created, identifier, lifespan, payload, status = toMessageStatus(now))

private[unimog] object MessageSqlSchema:
  def apply[A](message: Message.Create[A]): MessageSqlSchema[A] = MessageSqlSchema(
    completed = none,
    created = message.created,
    identifier = message.identifier,
    lifespan = message.lifespan,
    payload = message.payload,
    started = none
  )

  def codec[A](payload: Codec[A]): Codec[MessageSqlSchema[A]] =
    (instant.opt *: instant *: uuid *: interval *: payload *: instant.opt).to
