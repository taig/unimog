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

private[unimog] object MessageSqlQuery:
  def insert(schemas: NonEmptyList[MessageSqlSchema]): Command[Void] =
    val values = schemas.toList

    sql"""
    INSERT INTO "message" ("created", "identifier", "payload", "pending")
    VALUES ${MessageSqlSchema.codec.values.list(values)};
    """.command.contramap(_ => values)

  def select(limit: Int, stale: FiniteDuration): Query[Instant, MessageSqlSchema] =
    sql"""
    WITH "result" AS (
      SELECT "created", "identifier", "payload", "pending"
      FROM "message"
      WHERE "pending" IS NULL OR "pending" + '#${stale.toString}'::INTERVAL < $instant
      ORDER BY "created" ASC
      FOR UPDATE SKIP LOCKED
      LIMIT #${String.valueOf(limit)}
    )
    UPDATE "message"
    SET "pending" = $instant
    FROM "result"
    WHERE "message"."identifier" = "result"."identifier"
    RETURNING "result".*
    """.query(MessageSqlSchema.codec).contramap(instant => (instant, instant))

  val deleteByIdentifier: Command[UUID] =
    sql"""DELETE FROM "message" WHERE "identifier" = $uuid;""".command
