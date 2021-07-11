package refinery

import cats.data._
import cats.syntax.all._
import ujson._

class JsonTest extends munit.FunSuite {

  type V[A] = ValidatedC[String, String, A]

  implicit class JsonOps(value: Value) {
    def keyV(key: String): V[Value] =
      value.validC.context(key).andThen(_.objOpt.flatMap(_.get(key)).toValidatedC("No value found"))
    def strV: V[String] = value.strOpt.toValidatedC(s"Expected a string, found: $value")
    def intV: V[Int] = value.numOpt.toValidatedC(s"Expected a number, found: $value").map(_.toInt)
    def arrV: V[Vector[Value]] =
      value.arrOpt.toValidatedC(s"Expected array, found: $value").map(_.toVector)
  }

  implicit class VectorOps[A](value: Vector[A]) {
    def traverseWithIndexV[B](fn: A => V[B]): V[Vector[B]] = value.zipWithIndex.traverse {
      case (item, index) => item.validC.context(index.toString).andThen(fn)
    }
  }

  def parseConnection(data: Value): V[Connection] = (
    data.keyV("endpoint").andThen(_.strV),
    data.keyV("port").andThen(_.intV),
  ).mapN(Connection.apply)

  def parseConfig(data: Value): V[Cluster] = (
    data.keyV("leader").andThen(parseConnection),
    data.keyV("followers").andThen(_.arrV).andThen(_.traverseWithIndexV(parseConnection)),
  ).mapN(Cluster.apply)

  test("happy path") {
    val configData = Obj(
      "leader" -> Obj(
        "endpoint" -> "machine1",
        "port" -> 1234,
      ),
      "followers" -> Arr(
        Obj(
          "endpoint" -> "machine2",
          "port" -> 5678,
        ),
        Obj(
          "endpoint" -> "machine3",
          "port" -> 5678,
        ),
      ),
    )

    val result: V[Cluster] = parseConfig(configData)

    assertEquals(
      result.toEither,
      Right(
        Cluster(
          leader = Connection("machine1", 1234),
          followers = Vector(
            Connection("machine2", 5678),
            Connection("machine3", 5678),
          ),
        ),
      ),
    )
  }

  test("multiple failures") {
    val badData = Obj(
      "leader" -> Obj(
        "endpoint" -> "machine1",
        "port" -> "bad",
      ),
      "followers" -> Arr(
        Obj(
          "endpoint" -> "machine2",
          "port" -> "bad",
        ),
        Obj(
          "port" -> 2345,
        ),
      ),
    )

    val result: V[Cluster] = parseConfig(badData)

    def formatError(error: Error[String, String]): String =
      error.context.append(error.error).mkString_(": ")

    assertEquals(
      result.toEither.leftMap(_.map(formatError).toList),
      Left(
        List(
          "leader: port: Expected a number, found: \"bad\"",
          "followers: 0: port: Expected a number, found: \"bad\"",
          "followers: 1: endpoint: No value found",
        ),
      ),
    )
  }
}
