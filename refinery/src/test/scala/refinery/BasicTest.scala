package refinery

import cats.data.{Validated => CatsValidated, _}

class BasicTest extends munit.FunSuite {
  case class Connection(endpoint: String, port: Int)

  case class Cluster(
                    leader: Connection,
                    followers: Vector[Connection],
                    )

  def parseConfig(data: Map[String, String]): Validated[Cluster] = throw new Exception("Not implemented")

  test("happy path") {
    val configData = Map(
      "leader.endpoint" -> "machine1",
      "leader.port" -> "1234",
      "follower0.endpoint" -> "machine2",
      "follower0.port" -> "5678",
      "follower1.endpoint" -> "machine3",
      "follower1.port" -> "5678",
    )

    val result: Validated[Cluster] = parseConfig(configData)

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
      "follower0.endpoint" -> "machine2",
      "follower0.port" -> "bad",
      "follower1.port" -> "2345",
    )

    val result: Validated[Cluster] = parseConfig(badData)

    assertEquals(result.toEither, Left(NonEmptyChain(
      "leader: port: expected a number, found: 'bad'",
      "followers: 0: port: expected a number, found: 'bad'",
      "followers: 1: endpoint: missing"
    )))
  }
}
