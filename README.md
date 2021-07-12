# Refinery - parser for complex data structures

[![Maven Central](https://img.shields.io/maven-central/v/com.koterpillar/refinery_2.13)](https://mvnrepository.com/artifact/com.koterpillar/refinery)

This library is a refinement of Cats' [Validated] for representing the result
of parsing some input, to add more context about where any failures
occurred.

## Problem

* Using `Either` in Scala is a way to parse complex data structures and report
  errors. However, the first failure stops the process, and users need to fix
  it to see what else is wrong. Thus, failures have to be fixed one by one.
* Cats' `Validated` can parse multiple parts of a structure in parallel,
  reporting all the errors at once. However, when building a complex structure
  (for example, using `mapN`), the errors do not point to the exact part that
  failed.
* This library offers a `ValidatedC` data type that keeps track of context -
  the logical place in the structure that is currently being parsed. This
  context is added to errors so it's easy to pinpoint the problem.

## Related libraries comparison

| Library                                 | Data parsed  | Errors reported | Error context   |
| :------                                 | :----------  | :-------------- | :------------   |
| `Either` with a `for` comprehension     | ✅ Arbitrary | ⚠️  First        | ❌ None         |
| `Validated` with `mapN`                 | ✅ Arbitrary | ✅ All          | ❌ None         |
| [Circe]                                 | ⚠️  JSON      | ⚠️  First        | ✅ JSON path    |
| [kantan.csv]                            | ⚠️  CSV       | ⚠️  First        | ⚠️  CSV row only |
| [scala-parser-combinators], [FastParse] | ⚠️  Strings   | ⚠️  First        | ✅ Position     |
| Refinery                                | ✅ Arbitrary | ✅ All          | ✅ User defined |

## Motivational example

Let's try to parse a configuration into a simple case class:

```scala
import cats.implicits._

case class Box(width: Int, height: Int, depth: Int)

val config: Map[String, String] = Map(
  "width" -> "10",
  "height" -> "oops",
)
```

The configuration is invalid (height is not an integer, and depth is missing).
How will the errors be reported?

### Either

```scala
def parseInt(value: String): Either[String, Int] =
  value.toIntOption.toRight(s"Invalid integer: $value")

def getConfig(key: String): Either[String, String] =
  config.get(key).toRight(s"No key: $key")

println(
  (
    getConfig("width").flatMap(parseInt),
    getConfig("height").flatMap(parseInt),
    getConfig("depth").flatMap(parseInt),
  ).mapN(Box.apply)
)
// => Left(Invalid integer: oops)
```

Only the first error is reported.

### Cats' Validated

```scala
def parseInt(value: String): ValidatedNec[String, Int] =
  value.toIntOption.toValidNec(s"Invalid integer: $value")

def getConfig(key: String): ValidatedNec[String, String] =
  config.get(key).toValidNec(s"No key: $key")

println(
  (
    getConfig("width").andThen(parseInt),
    getConfig("height").andThen(parseInt),
    getConfig("depth").andThen(parseInt),
  ).mapN(Box.apply)
)
// => Invalid(Chain(Invalid integer: oops, No key: depth))
```

Both invalid height and missing depth errors are reported, but the height
error lacks context.

### Refinery

```scala
import refinery._

def parseInt(value: String): ValidatedC[String, String, Int] =
  value.toIntOption.toValidatedC(s"Invalid integer: $value")

def getConfig(key: String): ValidatedC[String, String, String] =
  config.get(key).toValidatedC("Key missing").context(key)

println(
  (
    getConfig("width").andThen(parseInt),
    getConfig("height").andThen(parseInt),
    getConfig("depth").andThen(parseInt),
  ).mapN(Box.apply)
)
// => Invalid(Chain(height: Invalid integer: oops, depth: Key missing))
```

Because `getConfig` now adds context to the validated value, the "Invalid
integer" error now specifies that the bad value was `height`.

[Circe]: https://circe.github.io/circe/
[FastParse]: https://com-lihaoyi.github.io/fastparse/
[kantan.csv]: https://nrinaudo.github.io/kantan.csv/
[scala-parser-combinators]: https://github.com/scala/scala-parser-combinators
[Validated]: https://typelevel.org/cats/datatypes/validated.html
