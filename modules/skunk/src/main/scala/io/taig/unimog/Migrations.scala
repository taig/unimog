package io.taig.unimog

object Migrations:
  def _1(schema: String): Migration = Migration(
    name = "Initial",
    sql = s"""CREATE SCHEMA IF NOT EXISTS "$schema";
             |
             |CREATE TABLE "message" (
             |  "identifier" UUID PRIMARY KEY,
             |  "created" TIMESTAMPTZ NOT NULL,
             |  "payload" TEXT NOT NULL,
             |  "pending" TIMESTAMPTZ NULL
             |);""".stripMargin
  )

  def all(schema: String): List[Migration] = List(_1(schema))
