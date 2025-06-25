package io.taig.unimog

object Migrations:
  val _1: Migration = Migration(
    name = "initial",
    sql = """CREATE TABLE "message" (
            |  "identifier" UUID PRIMARY KEY,
            |  "created" TIMESTAMPTZ NOT NULL,
            |  "payload" TEXT NOT NULL,
            |  "pending" TIMESTAMPTZ NULL
            |);""".stripMargin
  )

  val All: List[Migration] = List(_1)
