package refinery

import cats.Applicative
import cats.data.{Chain, NonEmptyChain}

sealed trait ValidatedC[+C, +E, +A]

object ValidatedC {
  final case class Valid[+C, +A](context: Chain[C], value: A) extends ValidatedC[C, Nothing, A]

  type Errors[+C, +E] = NonEmptyChain[Error[C, E]]
  final case class Invalid[+C, +E](errors: Errors[C, E]) extends ValidatedC[C, E, Nothing]

  implicit def applicative[C, E]: Applicative[ValidatedC[C, E, *]] =
    new Applicative[ValidatedC[C, E, *]] {
      override def pure[A](x: A): ValidatedC[C, E, A] = ValidatedC.Valid(Chain.empty[C], x)

      override def ap[A, B](
          ff: ValidatedC[C, E, A => B],
      )(fa: ValidatedC[C, E, A]): ValidatedC[C, E, B] = (ff, fa) match {
        case (Valid(c1, f), Valid(c2, a))   => Valid(c1 ++ c2, f(a))
        case (i1 @ Invalid(_), Valid(_, _)) => i1
        case (Valid(_, _), i2 @ Invalid(_)) => i2
        case (Invalid(e1), Invalid(e2))     => Invalid(e1 ++ e2)
      }
    }
}
