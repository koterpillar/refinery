package refinery

import cats.data._
import cats.syntax.all._

class BasicTest extends munit.FunSuite {
  case class Connection(endpoint: String, port: Int)

  case class Cluster(
                    leader: Connection,
                    followers: Vector[Connection],
                    )

  type Config = Map[String, String]

  implicit class ConfigOps(value: Config) {
    def project[A](prefix: String): ValidatedC[Config] = value.flatMap {
      case (k, v) if k.startsWith(prefix) => Some(k.substring(prefix.length).dropWhile(_ == '.') -> v)
      case _ => None
    }.validC.context(prefix)

    def only: ValidatedC[String] = value.get("") match {
      case Some(result) => result.validC
      case None => "No value found".invalidC
    }

    def topLevelKeys: Vector[String] = value.keys.map(_.takeWhile(_ != '.')).toVector
  }

  type Parser[A] = Config => ValidatedC[A]

  def parseInt(value: String): ValidatedC[Int] = value.toIntOption match {
    case Some(value) => value.validC
    case None => s"Expected a number, found: '$value'".invalidC
  }

  def manyOf[A](parser: Parser[A]): Parser[Vector[A]] = config => {
    val keys: Vector[String] = config.topLevelKeys.flatMap(_.toIntOption).sorted.map(_.toString)
    val items: Vector[ValidatedC[Config]] = keys.map(config.project)
    items.traverse(_.and(parser))
  }

  def parseConnection(data: Config): ValidatedC[Connection] = (
    data.project("endpoint").and(_.only),
    data.project("port").and(_.only).and(parseInt),
  ).mapN(Connection.apply)

  def parseConfig(data: Config): ValidatedC[Cluster] = (
    data.project("leader").and(parseConnection),
    data.project("followers").and(manyOf(parseConnection)),
  ).mapN(Cluster.apply)

  test("happy path") {
    val configData = Map(
      "leader.endpoint" -> "machine1",
      "leader.port" -> "1234",
      "followers.0.endpoint" -> "machine2",
      "followers.0.port" -> "5678",
      "followers.1.endpoint" -> "machine3",
      "followers.1.port" -> "5678",
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
    val badData = Map(
      "leader.endpoint" -> "machine1",
      "leader.port" -> "bad",
      "followers.0.endpoint" -> "machine2",
      "followers.0.port" -> "bad",
      "followers.1.port" -> "2345",
    )

    val result: ValidatedC[Cluster] = parseConfig(badData)

    assertEquals(result.toEither.leftMap(_.toList), Left(List(
      "leader: port: Expected a number, found: 'bad'",
      "followers: 0: port: Expected a number, found: 'bad'",
      "followers: 1: endpoint: No value found"
    )))
  }
}
