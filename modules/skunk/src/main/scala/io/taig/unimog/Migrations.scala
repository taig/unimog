package io.taig.unimog

final class Migrations(payloadType: String, schema: String):
  val _1: Migration = Migration(
    name = "Initial",
    sql = s"""CREATE SCHEMA IF NOT EXISTS "$schema";
             |
             |CREATE TABLE "$schema"."message" (
             |  "identifier" UUID PRIMARY KEY,
             |  "created" TIMESTAMPTZ NOT NULL,
             |  "payload" $payloadType NOT NULL,
             |  "pending" TIMESTAMPTZ NULL
             |);""".stripMargin
  )

  val all: List[Migration] = List(_1)
