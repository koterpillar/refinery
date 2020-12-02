import cats._
import cats.data.{Validated => CatsValidated, _}
import cats.syntax.all._

package object refinery {
  type Validated[A] = Nested[ValidatedF, Context, A]

  type ValidatedF[A] = ValidatedNec[String, A]
  type Context[A]    = (Chain[String], A)

  implicit class ValidatedOps[A](value: Validated[A]) {
    def toEither: Either[NonEmptyChain[String], A] = value.value match {
      case CatsValidated.Valid(a) => a._2.asRight[NonEmptyChain[String]]
      case CatsValidated.Invalid(e) => e.asLeft[A]
    }
  }
}
