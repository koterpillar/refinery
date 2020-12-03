import cats._
import cats.data._
import cats.syntax.all._

package object refinery {
  type ValidatedC[A] = Nested[ValidatedF, Context, A]

  type ValidatedF[A] = ValidatedNec[String, A]
  type Context[A]    = (Chain[String], A)

  private[refinery] def prependContext[A](context: Chain[String], value: ValidatedC[A]): ValidatedC[A] = value.value match {
    case Validated.Valid((ctx, a)) => validatedC((context ++ ctx, a).valid)
    case Validated.Invalid(e) => invalid(e.map(error => context.append(error).mkString_(": ")))
  }

  private[refinery] def validatedC[A](value: ValidatedF[Context[A]]): ValidatedC[A] = Nested[ValidatedF, Context, A](value)

  private[refinery] def invalid[A](value: NonEmptyChain[String]): ValidatedC[A] = validatedC(value.invalid)

  implicit class ValidatedOps[A](value: ValidatedC[A]) {
    def toEither: Either[NonEmptyChain[String], A] = value.value match {
      case Validated.Valid((_, a)) => a.asRight[NonEmptyChain[String]]
      case Validated.Invalid(e) => e.asLeft[A]
    }

    def context(context: String): ValidatedC[A] = value.value match {
      case Validated.Valid((ctx, a)) => validatedC((ctx.append(context), a).valid)
      case Validated.Invalid(e) => invalid(e)
    }

    def andThen[B](fn: A => ValidatedC[B]): ValidatedC[B] = value.value match {
      case Validated.Valid((ctx, a)) => prependContext(ctx, fn(a))
      case Validated.Invalid(e) => invalid(e)
    }
  }

  implicit class ValueOps[A](value: A) {
    def validC: ValidatedC[A] = value.pure[ValidatedC]
  }

  implicit class OptionOps[A](value: Option[A]) {
    def toValidatedC(error: => String): ValidatedC[A] = value match {
      case None => error.invalidC
      case Some(value) => value.validC
    }
  }

  implicit class EitherOps[A](value: Either[String, A]) {
    def toValidatedC: ValidatedC[A] = value match {
      case Left(error) => error.invalidC
      case Right(value) => value.validC
    }
  }

  implicit class FailureOps(value: String) {
    def invalidC[A]: ValidatedC[A] = invalid(NonEmptyChain(value))
  }
}
