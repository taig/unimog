package io.taig.unimog

import cats.Functor
import cats.effect.Clock
import cats.syntax.all.*

import java.time.Instant

private[unimog] def realTimeInstant[F[_]: Functor](using clock: Clock[F]): F[Instant] =
  clock.realTime.map(duration => Instant.EPOCH.plusNanos(duration.toNanos))
