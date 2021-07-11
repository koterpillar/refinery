package refinery

import cats.data._
import cats.syntax.all._

class BasicTest extends munit.FunSuite {
  type Config = Map[String, String]

  type V[A] = ValidatedC[String, String, A]

  implicit class ConfigOps(value: Config) {
    def project[A](prefix: String): V[Config] = value
      .flatMap {
        case (k, v) if k == prefix                => Some("" -> v)
        case (k, v) if k.startsWith(prefix + ".") => Some(k.substring(prefix.length + 1) -> v)
        case _                                    => None
      }
      .validC
      .context(prefix)

    def only: V[String] = value.get("") match {
      case Some(result) => result.validC
      case None         => "No value found".invalidC
    }

    def topLevelKeys: Vector[String] = value.keys.map(_.takeWhile(_ != '.')).toVector.distinct
  }

  type Parser[A] = Config => V[A]

  def parseInt(value: String): V[Int] = value.toIntOption match {
    case Some(value) => value.validC
    case None        => s"Expected a number, found: '$value'".invalidC
  }

  def manyOf[A](parser: Parser[A]): Parser[Vector[A]] = config => {
    val keys: Vector[String] =
      config.topLevelKeys.filter(_.toIntOption.nonEmpty).sortBy(_.toIntOption)
    val items: Vector[V[Config]] = keys.map(config.project)
    items.traverse(_.andThen(parser))
  }

  def parseConnection(data: Config): V[Connection] = (
    data.project("endpoint").andThen(_.only),
    data.project("port").andThen(_.only).andThen(parseInt),
  ).mapN(Connection.apply)

  def parseConfig(data: Config): V[Cluster] = (
    data.project("leader").andThen(parseConnection),
    data.project("followers").andThen(manyOf(parseConnection)),
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
    val badData = Map(
      "leader.endpoint" -> "machine1",
      "leader.port" -> "bad",
      "followers.0.endpoint" -> "machine2",
      "followers.0.port" -> "bad",
      "followers.1.port" -> "2345",
    )

    val result: V[Cluster] = parseConfig(badData)

    def formatError(error: Error[String, String]): String =
      error.context.append(error.error).mkString_(": ")

    assertEquals(
      result.toEither.leftMap(_.map(formatError).toList),
      Left(
        List(
          "leader: port: Expected a number, found: 'bad'",
          "followers: 0: port: Expected a number, found: 'bad'",
          "followers: 1: endpoint: No value found",
        ),
      ),
    )
  }
}
