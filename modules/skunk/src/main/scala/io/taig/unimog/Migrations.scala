package io.taig.unimog

final class Migrations(payloadType: String, schema: String):
  val _1: Migration = Migration(
    name = "Initial",
    sql = s"""CREATE SCHEMA IF NOT EXISTS "$schema";
             |
             |CREATE TABLE "$schema"."message" (
             |  "completed" TIMESTAMP NULL,
             |  "created" TIMESTAMP NOT NULL,
             |  "expiration" TIMESTAMP NULL GENERATED ALWAYS AS ("started" + "lifespan") STORED,
             |  "identifier" UUID PRIMARY KEY,
             |  "lifespan" INTERVAL NOT NULL,
             |  "payload" $payloadType NOT NULL,
             |  "started" TIMESTAMP NULL
             |);
             |
             |CREATE INDEX ON "$schema"."message" ("created" ASC) WHERE "started" IS NULL;
             |CREATE INDEX ON "$schema"."message" ("expiration" ASC, "created" ASC) WHERE "started" IS NOT NULL;""".stripMargin
  )

  val all: List[Migration] = List(_1)
