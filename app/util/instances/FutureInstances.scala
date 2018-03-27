package util.instances

import scala.concurrent.{ExecutionContext, Future}

import util.Monad

trait FutureInstances {

  implicit def futureInstance(implicit ec: ExecutionContext): Monad[Future] =
    new Monad[Future] {
      override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)
      override def fmap[A, B](m: Future[A], f: A => B): Future[B] = m.map(f)
      override def apply[A, B](mf: Future[A => B], ma: Future[A]): Future[B] = mf.flatMap(f => ma.map(f))
      override def pure[A](a: A): Future[A] = Future.successful(a)
    }

}
