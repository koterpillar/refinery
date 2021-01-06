import cats._
import cats.data._
import cats.syntax.all._

package object refinery {
  private[refinery] def prependContext[C, E, A](context: Chain[C], value: ValidatedC[C, E, A]): ValidatedC[C, E, A] = value match {
    case ValidatedC.Valid(ctx, a) => ValidatedC.Valid(context ++ ctx, a)
    case ValidatedC.Invalid(errors) => ValidatedC.Invalid(errors.map { case (ctx, e) => (context ++ ctx, e) })
  }

  private[refinery] def invalid[C, E, A](value: NonEmptyChain[E]): ValidatedC[C, E, A] = ValidatedC.Invalid(value.map(Chain.empty[C] -> _))

  implicit class ValidatedOps[C, E, A](value: ValidatedC[C, E, A]) {
    def toEither: Either[ValidatedC.Errors[C, E], A] = value.value match {
      case ValidatedC.Valid(_, a) => a.asRight[ValidatedC.Errors[C, E]]
      case ValidatedC.Invalid(errors) => errors.asLeft[A]
    }

    def context(context: C): ValidatedC[C, E, A] = value match {
      case ValidatedC.Valid(ctx, a) => ValidatedC.Valid(ctx.append(context), a)
      case invalid@ValidatedC.Invalid(_) => invalid
    }

    def andThen[B](fn: A => ValidatedC[C, E, B]): ValidatedC[C, E, B] = value.value match {
      case ValidatedC.Valid(ctx, a) => prependContext(ctx, fn(a))
      case invalid@ValidatedC.Invalid(_) => invalid
    }
  }

  implicit class ValueOps[A](value: A) {
    def validC[C, E]: ValidatedC[C, E, A] = value.pure[ValidatedC[C, E, *]]
  }

  implicit class OptionOps[A](value: Option[A]) {
    def toValidatedC[C, E](error: => E): ValidatedC[C, E, A] = value match {
      case None => error.invalidC[C, A]
      case Some(value) => value.validC
    }
  }

  implicit class EitherOps[E, A](value: Either[E, A]) {
    def toValidatedC[C]: ValidatedC[C, E, A] = value match {
      case Left(error) => error.invalidC
      case Right(value) => value.validC
    }
  }

  implicit class FailureOps[E](value: E) {
    def invalidC[C, A]: ValidatedC[C, E, A] = invalid(NonEmptyChain(value))
  }
}
