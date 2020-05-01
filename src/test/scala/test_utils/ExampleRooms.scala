package test_utils

import server.communication.ConnectionConfigurations
import server.room._

/**
 * Rooms used for testing purpose.
 */
object ExampleRooms {

  object RoomWithState {
<<<<<<< HEAD
    def apply(): RoomWithState = new RoomWithState()
    val name = "roomWithState"
    val UpdateRate = 100 //milliseconds
    val RoomInitialState: Int = 0
  }

  class RoomWithState() extends ServerRoom with SynchronizedRoomState[Integer] {
=======
    val Name = "roomWithState"
    val UpdateRate = 100 //milliseconds
    val RoomInitialState: Int = 0
  }
  case class RoomWithState() extends ServerRoom with SynchronizedRoomState[Integer] {
>>>>>>> upstream/develop
    private var internalState = RoomWithState.RoomInitialState
    override val stateUpdateRate: Int = RoomWithState.UpdateRate
    override def onCreate(): Unit = {}
    override def onClose(): Unit = this.stopStateSynchronization()
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {}
    override def currentState: Integer = this.internalState

    //Only used for testing
    def changeState(newState: Int): Unit = this.internalState = newState
  }

<<<<<<< HEAD
  //_________________________________________________

  object RoomWithGameLoop {
    def apply(): RoomWithGameLoop = new RoomWithGameLoop()
    val name = "roomWithGameLoop"
    val initialState = 0
    val updateRate = 100 // millis
  }

  class RoomWithGameLoop() extends ServerRoom with GameLoop {
=======
  object RoomWithGameLoop {
    val Name = "roomWithGameLoop"
    val initialState = 0
    val updateRate = 100 // millis
  }
  case class RoomWithGameLoop() extends ServerRoom with GameLoop {
>>>>>>> upstream/develop

    private var count = RoomWithGameLoop.initialState
    override val worldUpdateRate: Int = RoomWithGameLoop.updateRate

    override def joinConstraints: Boolean = true

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def updateWorld(elapsed: Long): Unit = {
      count = count + 1
      receivedTicks = receivedTicks + 1
    }

    // Used for testing purpose
    def state: Int = count

    var receivedTicks: Int = 0
  }

<<<<<<< HEAD
  //__________________________________________________

  object RoomWithReconnection {
    def apply(): RoomWithReconnection = new RoomWithReconnection()
    val name = "roomWithReconnection"
  }

  class RoomWithReconnection() extends ServerRoom {
=======
  object RoomWithReconnection {
    val Name = "roomWithReconnection"
  }
  case class RoomWithReconnection() extends ServerRoom {
>>>>>>> upstream/develop
    private val ReconnectionTime = 10 //s
    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {
      this.allowReconnection(client, ReconnectionTime)
    }

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = true
  }

<<<<<<< HEAD
  //________________________________________________

  object ClosableRoomWithState {
    def apply(): ClosableRoomWithState = new ClosableRoomWithState()
    val name = "closableRoomWithState"
=======
  object ClosableRoomWithState {
    val Name = "closableRoomWithState"
>>>>>>> upstream/develop
    val ChangeStateMessage = "changeState"
    val CloseRoomMessage = "close"
    val PingMessage = "ping"
    val PongResponse = "pong"
  }
<<<<<<< HEAD

  class ClosableRoomWithState() extends ServerRoom with SynchronizedRoomState[String] {
=======
  case class ClosableRoomWithState() extends ServerRoom with SynchronizedRoomState[String] {

>>>>>>> upstream/develop
    import ClosableRoomWithState._

    import scala.concurrent.duration._
    override val stateUpdateRate = 200
    override val socketConfigurations: ConnectionConfigurations = ConnectionConfigurations(2 seconds)
    private var gameState = "gameState"

    override def onCreate(): Unit = this.startStateSynchronization()

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {
      message.toString match {
        case CloseRoomMessage => this.close()
        case PingMessage => this.tell(client, PongResponse)
        case ChangeStateMessage => this.gameState = "gameState_updated"
      }
    }

    override def currentState: String = this.gameState

    override def joinConstraints: Boolean = true

  }

<<<<<<< HEAD
  //________________________________________________

  object NoPropertyRoom {
    def apply(): NoPropertyRoom = new NoPropertyRoom()
    val name = "noProperty"
  }

  class NoPropertyRoom() extends ServerRoom {
=======
  object NoPropertyRoom {
    val Name = "noProperty"
  }
  case class NoPropertyRoom() extends ServerRoom {
>>>>>>> upstream/develop

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = {
      true
    }
  }

<<<<<<< HEAD
  //________________________________________________

  object RoomWithProperty {
    def apply(): RoomWithProperty = new RoomWithProperty()
    val name = "roomWithProperty"
  }

  class RoomWithProperty() extends ServerRoom {
=======
  object RoomWithProperty {
    val Name = "roomWithProperty"
  }
  //noinspection ScalaUnusedSymbol
  case class RoomWithProperty() extends ServerRoom {
>>>>>>> upstream/develop

    @RoomPropertyMarker private val a: Int = 0 //noinspection unused
    @RoomPropertyMarker private val b: String = "abc"
    private val c: Int = 0

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: server.room.Client): Unit = {}

    override def onLeave(client: server.room.Client): Unit = {}

    override def onMessageReceived(client: server.room.Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = {
      true
    }
  }

<<<<<<< HEAD
  // ____________________________________________________________________

  object RoomWithProperty2 {
    def apply(): RoomWithProperty2 = new RoomWithProperty2()
    val name = "roomWithProperty2"
  }

  class RoomWithProperty2() extends ServerRoom {
=======
  object RoomWithProperty2 {
    val Name = "roomWithProperty2"
  }
  //noinspection ScalaUnusedSymbol
  case class RoomWithProperty2() extends ServerRoom {
>>>>>>> upstream/develop

    @RoomPropertyMarker private var a: Int = 1
    @RoomPropertyMarker private var b: String = "a"
    @RoomPropertyMarker private var c: Boolean = true
    private var d: Int = 0

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = true
  }

<<<<<<< HEAD
  // ___________________________________________________________________

  object LockableRoom {
    def locked(): LockableRoom = new LockableRoom(true)
    def unlocked(): LockableRoom = new LockableRoom(false)
    val lockedName = "lockedRoom"
    val unlockedName = "unlockedRoom"
  }

  class LockableRoom(private val _isLocked: Boolean) extends ServerRoom {
=======
  object LockableRoom {
    val LockedRoomType = "lockedRoom"
    val UnlockedRoomType = "unlockedRoom"
  }
  case class LockableRoom(private val _isLocked: Boolean) extends ServerRoom {
>>>>>>> upstream/develop

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def isLocked: Boolean = _isLocked
  }
<<<<<<< HEAD
=======

>>>>>>> upstream/develop
}
