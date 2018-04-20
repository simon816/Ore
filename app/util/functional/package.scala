package util

package object functional {

  type Id[A] = A

  implicit val idInstance: Monad[Id] = new Monad[Id] {
    override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
    override def pure[A](a: A): Id[A] = a

    override def ap[A, B](ff: Id[A => B])(fa: Id[A]): Id[B] = ff(fa)
    override def flatten[A](ffa: Id[Id[A]]): Id[A] = ffa
    override def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)
    override def product[A, B](fa: Id[A], fb: Id[B]): (A, B) = (fa, fb)
    override def map2[A, B, C](fa: Id[A], fb: Id[B])(f: (A, B) => C): Id[C] = f(fa, fb)
    override def <*>[A, B](ff: Id[A => B])(fa: Id[A]): Id[B] = ff(fa)
    override def *>[A, B](fa: Id[A])(fb: Id[B]): Id[B] = fb
    override def <*[A, B](fa: Id[A])(fb: Id[B]): Id[A] = fa
    override def as[A, B](fa: Id[A], b: B): Id[B] = b
    override def fproduct[A, B](fa: Id[A])(f: A => B): (A, B) = (fa, f(fa))
    override def tupleLeft[A, B](fa: Id[A], b: B): (B, A) = (b, fa)
    override def tupleRight[A, B](fa: Id[A], b: B): (A, B) = (fa, b)
  }

}
