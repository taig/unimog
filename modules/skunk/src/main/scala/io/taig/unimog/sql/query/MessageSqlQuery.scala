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
import skunk.Codec

final private[unimog] class MessageSqlQuery[A](payload: Codec[A], schema: String):
  def insert(schemas: NonEmptyList[MessageSqlSchema[A]]): Command[Void] =
    val values = schemas.toList

    sql"""
    INSERT INTO "#$schema"."message" ("completed", "created", "identifier", "lifespan", "payload", "started")
    VALUES ${MessageSqlSchema.codec(payload).values.list(values)};
    """.command.contramap(_ => values)

  def select(limit: Int): Query[Instant, MessageSqlSchema[A]] =
    sql"""
    WITH "result" AS (
      SELECT "completed", "created", "identifier", "lifespan", "payload", "started"
      FROM "#$schema"."message"
      WHERE "started" IS NULL OR "expiration" < $instant
      ORDER BY "created" ASC
      FOR UPDATE SKIP LOCKED
      LIMIT #${String.valueOf(limit)}
    )
    UPDATE "#$schema"."message"
    SET "started" = $instant
    FROM "result"
    WHERE "#$schema"."message"."identifier" = "result"."identifier"
    RETURNING "result".*
    """.query(MessageSqlSchema.codec(payload)).contramap(instant => (instant, instant))

  val updateCompletedByIdentifier: Command[(Instant, UUID)] =
    sql"""
    UPDATE "#$schema"."message"
    SET "completed" = $instant
    WHERE "identifier" = $uuid;
    """.command
