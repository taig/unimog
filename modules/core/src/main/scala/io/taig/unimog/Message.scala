package io.taig.unimog

import java.time.Instant
import java.util.UUID

final case class Message(created: Instant, identifier: UUID, payload: String)
