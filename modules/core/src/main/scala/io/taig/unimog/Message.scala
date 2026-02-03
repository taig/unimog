package io.taig.unimog

import java.time.Instant
import java.util.UUID
import java.time.Duration

final case class Message[A](created: Instant, identifier: UUID, lifespan: Duration, payload: A, status: Message.Status):
  def completed: Option[Instant] =
    PartialFunction.condOpt(status) { case Message.Status.Completed(_, finished) => finished }

object Message:
  enum Status:
    case Completed(started: Instant, finished: Instant)
    case Pending
    case Processing(started: Instant)
    case Rescheduled(expired: Instant)

  final case class Create[A](created: Instant, identifier: UUID, lifespan: Duration, payload: A):
    def toMessage(status: Message.Status): Message[A] =
      Message(created, identifier, lifespan, payload, status)
