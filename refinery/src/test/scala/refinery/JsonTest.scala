package refinery

import cats.data._
import cats.syntax.all._
import ujson._

class JsonTest extends munit.FunSuite {

  implicit class JsonOps(value: Value) {
    def keyV(key: String): ValidatedC[Value] = value.validC.context(key).andThen(_.objOpt.flatMap(_.get(key)).toValidatedC("No value found"))
    def strV: ValidatedC[String] = value.strOpt.toValidatedC(s"Expected a string, found: $value")
    def intV: ValidatedC[Int] = value.numOpt.toValidatedC(s"Expected a number, found: $value").map(_.toInt)
    def arrV: ValidatedC[Vector[Value]] = value.arrOpt.toValidatedC(s"Expected array, found: $value").map(_.toVector)
  }

  implicit class VectorOps[A](value: Vector[A]) {
    def traverseWithIndexV[B](fn: A => ValidatedC[B]): ValidatedC[Vector[B]] = value.zipWithIndex.traverse {
      case (item, index) => item.validC.context(index.toString).andThen(fn)
    }
  }

  def parseConnection(data: Value): ValidatedC[Connection] = (
    data.keyV("endpoint").andThen(_.strV),
    data.keyV("port").andThen(_.intV),
  ).mapN(Connection.apply)

  def parseConfig(data: Value): ValidatedC[Cluster] = (
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

    val result: ValidatedC[Cluster] = parseConfig(configData)

    assertEquals(result.toEither, Right(
      Cluster(
        leader = Connection("machine1", 1234),
        followers = Vector(
          Connection("machine2", 5678),
          Connection("machine3", 5678),
        )
      )
    ))
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

    val result: ValidatedC[Cluster] = parseConfig(badData)

    assertEquals(result.toEither.leftMap(_.toList), Left(List(
      "leader: port: Expected a number, found: \"bad\"",
      "followers: 0: port: Expected a number, found: \"bad\"",
      "followers: 1: endpoint: No value found"
    )))
  }
}
