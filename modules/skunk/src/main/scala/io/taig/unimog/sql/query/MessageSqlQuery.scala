package io.taig.unimog.sql.query

import cats.data.NonEmptyList
import io.taig.unimog.sql.codecs.*
import io.taig.unimog.sql.schema.MessageSqlSchema
import skunk.Command
import skunk.Query
import skunk.Void
import skunk.codec.all.*
import skunk.syntax.all.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.FiniteDuration
import skunk.Codec

final private[unimog] class MessageSqlQuery[A](payload: Codec[A], schema: String):
  def insert(schemas: NonEmptyList[MessageSqlSchema[A]]): Command[Void] =
    val values = schemas.toList

    sql"""
    INSERT INTO "#$schema"."message" ("created", "identifier", "payload", "pending")
    VALUES ${MessageSqlSchema.codec(payload).values.list(values)};
    """.command.contramap(_ => values)

  def select(limit: Int, stale: FiniteDuration): Query[Instant, MessageSqlSchema[A]] =
    sql"""
    WITH "result" AS (
      SELECT "created", "identifier", "payload", "pending"
      FROM "#$schema"."message"
      WHERE "pending" IS NULL OR "pending" + '#${stale.toString}'::INTERVAL < $instant
      ORDER BY "created" ASC
      FOR UPDATE SKIP LOCKED
      LIMIT #${String.valueOf(limit)}
    )
    UPDATE "#$schema"."message"
    SET "pending" = $instant
    FROM "result"
    WHERE "#$schema"."message"."identifier" = "result"."identifier"
    RETURNING "result".*
    """.query(MessageSqlSchema.codec(payload)).contramap(instant => (instant, instant))

  val deleteByIdentifier: Command[UUID] =
    sql"""DELETE FROM "#$schema"."message" WHERE "identifier" = $uuid;""".command
