import cats._
import cats.data._
import cats.syntax.all._

package object refinery {
  type Validated[A] = Nested[ValidatedF, Context, A]

  type ValidatedF[A] = ValidatedNel[String, A]
  type Context[A]    = (Chain[String], A)
}
