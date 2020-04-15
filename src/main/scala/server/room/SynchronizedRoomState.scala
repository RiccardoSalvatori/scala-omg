package server.room


import common.communication.CommunicationProtocol.RoomProtocolMessage
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.room.RoomProperty
import server.room.RoomActor.StateSyncTick
import server.utils.Timer

/**
 * Trait that define a room with a state.
 *
 * @tparam T generic type for the state. It must extends [[java.io.Serializable]] so that it can be serialized and
 *           sent to clients
 */
trait SynchronizedRoomState[T <: Any with java.io.Serializable] { self: ServerRoom =>

  private val stateTimer = new Timer{ }

  /**
   * How often clients will be updated (time expressed in milliseconds)
   */
  protected val stateUpdateRate = 50 //milliseconds

  /**
   * Start sending state to all clients
   */
  def startStateUpdate(): Unit =
    stateTimer.scheduleAtFixedRate(() => generateStateSyncTick(), 0, stateUpdateRate)

  /**
   * Stop sending state updates to clients
   */
  def stopStateUpdate(): Unit = stateTimer.stopTimer()

  /**
   * This is the function that is called at each update to get the most recent state that will be sent to clients
   *
   * @return the current state of the game
   */
  def currentState: T

  private def generateStateSyncTick(): Unit =
    self.roomActor ! StateSyncTick(c => c send RoomProtocolMessage(StateUpdate, c.id, currentState))

}

object SynchronizedRoomState {

  private case class BasicServerRoomWithSynchronizedState() extends ServerRoom with SynchronizedRoomState[Integer] {
    override def joinConstraints: Boolean = true
    override def onCreate(): Unit = {}
    override def onClose(): Unit = {}
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {}
    override def currentState: Integer = 0
  }

  private def apply(): BasicServerRoomWithSynchronizedState = BasicServerRoomWithSynchronizedState()

  /**
   * Getter of the synchronized state properties
   *
   * @return a set containing the defined properties
   */
  def defaultProperties: Set[RoomProperty] = ServerRoom propertyDifferenceFrom BasicServerRoomWithSynchronizedState()
}







