package refinery

import cats.Applicative
import cats.data.{Chain, NonEmptyChain}

sealed trait ValidatedC[+C, +E, +A]

object ValidatedC {
  type Error[C, E] = (Chain[C], E)
  type Errors[C, E] = NonEmptyChain[Error[C, E]]
  final case class Valid[+C, +A](context: Chain[C], value: A) extends ValidatedC[C, Nothing, A]
  final case class Invalid[+C, +E](errors: Errors[C, E]) extends ValidatedC[C, E, Nothing]

  implicit def applicative[C, E]: Applicative[ValidatedC[C, E, *]] = ???
}
