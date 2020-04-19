package examples.moneygrabber.common

import examples.moneygrabber.common.Entities.{Coin, Direction, Directions, Hunter, Player, Position}
import Entities._

import scala.util.Random


object Board {

  private val CoinBasicValue = 10
  private val HunterVelocity = 3.5


  def withRandomCoins(size: (Int, Int), numHunters: Int, coinRatio: Double): Board = {
    val huntersList = hunters(size, numHunters)
    println(huntersList)
    Board(List.empty,
      randomCoins(size, coinRatio),
      huntersList,
      size)
  }

  private def randomCoins(worldSize: (Int, Int), coinRatio: Double): List[Coin] = {
    val pairs = for (x <- 0 until worldSize._1; y <- 0 until worldSize._2) yield (x, y)
    Random.shuffle(pairs).take((pairs.size * coinRatio).toInt).map(Coin(_, CoinBasicValue)).toList
  }

  private def hunters(size: (Int, Int), numHunters: Int): List[Hunter] = {
    (0 until numHunters).map(_ => Hunter((size._1 / 2, size._2 / 2), RandomDirection, HunterVelocity)).toList
  }
}

@SerialVersionUID(1111L) // scalastyle:ignore magic.number
case class Board(players: List[Player], coins: List[Coin], hunters: List[Hunter], size: (Int, Int))
  extends java.io.Serializable {

  def gameEnded: Boolean = this.coins.isEmpty || this.players.size == 1

  def winner: Player = if (this.players.size == 1) {
    this.players.head
  } else {
    this.players.max
  }

  def addPlayer(player: Player): Board = {
    if (this.players.exists(_.id == player.id)) {
      throw new IllegalStateException(s"There's already a player with id:${player.id}")
    }
    this.copy(players = this.players :+ player)
  }

  def removePlayer(id: Int): Board = {
    this.copy(players = this.players.filter(_.id != id))
  }

  def movePlayer(id: Int, direction: Direction): Board = {
    this.copy(players = this.players.map(p => {
      if (p.id == id) {
        Player(p.id, this.keepInsideBorders(p.move(direction)), p.points)
      } else {
        p
      }
    })).takeCoins().catchPlayers()
  }

  //Return the board with hunters that moved randomly
  def moveHunters(elapsed: Long): Board = {
    this.copy(hunters = this.hunters.map(h => {
      val next = h.copy(continuousPosition = h.moveContinuous(elapsed))
      if (next.position == h.position) {
        next
      } else {
        next.copy(continuousPosition = this.keepInsideBorders(next.position),
          currentDirection = if (Random.nextDouble > 0.8) RandomDirection else next.currentDirection)
      }
    })).catchPlayers()
  }

  //Return the world with players score updated according to taken coins
  def takeCoins(): Board = {
    val newCoins = coins.filter(c => !players.exists(p => p.position == c.position))
    val newPlayers = players.map(p => coins.find(c => c.position == p.position) match {
      case Some(coin) => Player(p.id, p.position, p.points + coin.value)
      case None => p
    })
    this.copy(players = newPlayers, coins = newCoins)
  }

  //Return the board without the catched players
  def catchPlayers(): Board = {
    this.copy(players = this.players.filter(!playerCatched(_)))
  }


  private def playerCatched(player: Player) = hunters.map(_.position).contains(player.position)

  private def keepInsideBorders(position: Position): Position = {
    (keepXInsideBorders(position._1), keepYInsideBorders(position._2))
  }

  private def keepXInsideBorders(x: Int) = {
    Math.min(Math.max(0, x), this.size._1 - 1)
  }

  private def keepYInsideBorders(y: Int) = {
    Math.min(Math.max(0, y), this.size._2 - 1)
  }
}