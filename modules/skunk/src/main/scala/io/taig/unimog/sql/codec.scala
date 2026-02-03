package io.taig.unimog.sql

import skunk.Codec
import skunk.codec.all.*

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private[unimog] object codecs:
  val instant: Codec[Instant] = timestamp.imap(_.toInstant(ZoneOffset.UTC))(LocalDateTime.ofInstant(_, ZoneOffset.UTC))
