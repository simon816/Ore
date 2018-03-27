package util

import scala.language.higherKinds

package object syntax extends ParallelSyntax {

  implicit class MonadOps[F[_], A](private val fa: F[A]) extends AnyVal {
    def flatMap[B](f: A => F[B])(implicit F: Monad[F]): F[B] = F.flatMap(fa)(f)
  }

  implicit class MonadFlattenOps[F[_], A](private val ffa: F[F[A]]) extends AnyVal {
    def flatten(implicit F: Monad[F]): F[A] = F.flatten(ffa)
  }
}
