package util

import scala.language.higherKinds

import play.api.libs.functional.{Applicative, Functor}

trait Monad[F[_]] extends Functor[F] with Applicative[F] {

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  override def map[A, B](fa: F[A], f: A => B): F[B] = fmap(fa, f)

  def flatten[A](ffa: F[F[A]]): F[A] = flatMap(ffa)(fa => fa)
}