package refinery

import cats.data.Chain

final case class Error[+C, +E](context: Chain[C], error: E) {
  def prependContext[CC >: C](preContext: Chain[CC]): Error[CC, E] = copy(context = preContext ++ context)
}

object Error {
  def of[C, E](error: E): Error[C, E] = Error(Chain.empty[C], error)
}
