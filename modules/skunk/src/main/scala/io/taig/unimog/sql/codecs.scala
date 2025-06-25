package io.taig.unimog.sql

import skunk.Codec
import skunk.codec.all.*

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

private[unimog] object codecs:
  val instant: Codec[Instant] = timestamptz.imap(_.toInstant)(OffsetDateTime.ofInstant(_, ZoneOffset.UTC))
