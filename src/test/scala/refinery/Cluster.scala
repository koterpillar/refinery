package refinery

case class Cluster(
    leader: Connection,
    followers: Vector[Connection],
)
