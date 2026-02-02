package io.taig.unimog

import cats.Applicative
import cats.Eval
import cats.Traverse
import cats.syntax.all.*

final case class Acknowledgable[F[_], A](ack: F[Unit], value: A):
  def map[B](f: A => B): Acknowledgable[F, B] = copy(value = f(value))

  def mapK[G[_]](fK: [A] => F[A] => G[A]): Acknowledgable[G, A] = copy(ack = fK(ack))

object Acknowledgable:
  given [F[_]]: Traverse[Acknowledgable[F, *]] with
    override def map[A, B](fa: Acknowledgable[F, A])(f: A => B): Acknowledgable[F, B] = fa.map(f)
    override def foldLeft[A, B](fa: Acknowledgable[F, A], b: B)(f: (B, A) => B): B = f(b, fa.value)
    override def foldRight[A, B](fa: Acknowledgable[F, A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      f(fa.value, lb)
    override def traverse[G[_]: Applicative, A, B](fa: Acknowledgable[F, A])(f: A => G[B]): G[Acknowledgable[F, B]] =
      f(fa.value).map(b => fa.copy(value = b))
