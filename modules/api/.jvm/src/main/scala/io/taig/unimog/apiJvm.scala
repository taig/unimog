package io.taig.unimog

import cats.effect.Clock

import java.time.Instant

private[unimog] def realTimeInstant[F[_]](using clock: Clock[F]): F[Instant] = clock.realTimeInstant
