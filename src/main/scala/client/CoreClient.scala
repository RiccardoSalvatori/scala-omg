package client

import akka.actor.{ActorRef, Stash}
import akka.util.Timeout
import client.room.{ClientRoom, JoinedRoom}
import client.utils.MessageDictionary._
import common.room.{Room, RoomJsonSupport, RoomProperty, RoomPropertyValue}

import scala.concurrent.duration._
import scala.util.{Failure, Success}


sealed trait CoreClient extends BasicActor

object CoreClient {

  import akka.actor.Props

  def apply(httpServerUri: String): Props = Props(classOf[CoreClientImpl], httpServerUri)
}

class CoreClientImpl(private val httpServerUri: String) extends CoreClient with RoomJsonSupport with Stash {

  private implicit val timeout: Timeout = 5 seconds
  private val httpClient = context.system actorOf HttpClient(httpServerUri)
  private var joinedRoomsActors: Set[ActorRef] = Set()

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitHttpResponse(replyTo: ActorRef): Receive = onWaitHttpResponse(replyTo) orElse fallbackReceive

  import common.room.RoomPropertyValueConversions._
  val onReceive: Receive = {

    case FailResponse(ex) => sender ! Failure(ex)

    case CreatePublicRoom(roomType, roomOptions) =>
      context.become(this.waitHttpResponse(sender))
      this.httpClient ! HttpPostRoom(roomType, roomOptions)

    case CreatePrivateRoom(roomType, roomOptions, password) =>
      context become this.waitHttpResponse(sender)
      httpClient ! HttpPostRoom(roomType, roomOptions + RoomProperty(Room.roomPasswordPropertyName, password.toString))

    case GetAvailableRooms(roomType, roomOptions) =>
      context.become(this.waitHttpResponse(sender))
      this.httpClient ! HttpGetRooms(roomType, roomOptions)


    case GetJoinedRooms =>
      if (this.joinedRoomsActors.isEmpty) {
        sender ! JoinedRooms(Set.empty)
      } else {
        context.become(roomsAggregator(this.joinedRoomsActors.size, sender, Set.empty))
        this.joinedRoomsActors.foreach(_ ! RetrieveClientRoom)
      }


    case ClientRoomActorLeft =>
      this.joinedRoomsActors = this.joinedRoomsActors - sender

    case ClientRoomActorJoined =>
      this.joinedRoomsActors = this.joinedRoomsActors + sender
  }


  /**
   * Behavior to handle response from HttpClientActor
   */
  def onWaitHttpResponse(replyTo: ActorRef): Receive = {

    case FailResponse(ex) =>
      context.become(onReceive)
      replyTo ! Failure(ex)
      unstashAll()

    case HttpRoomSequenceResponse(rooms) =>
      context.become(onReceive)
      replyTo ! Success(rooms.map(room =>
        ClientRoom(
          self,
          httpServerUri,
          room.roomId,
          room.properties
        )
      ))
      unstashAll()


    case HttpRoomResponse(room) =>
      context.become(onReceive)
      replyTo ! Success(
        ClientRoom(
          self,
          httpServerUri,
          room.roomId,
          room.properties
        )
      )
      unstashAll()

    case _ => stash
  }

  /**
   * Aggregate results to get all joined rooms
   */
  def roomsAggregator(expectedMessages: Int, replyTo: ActorRef, response: Set[JoinedRoom]): Receive = {
    case ClientRoomResponse(room) =>
      if (expectedMessages == 1) {
        replyTo ! JoinedRooms(response + room)
        context.become(receive)
      } else {
        context.become(roomsAggregator(expectedMessages - 1, replyTo, response + room))
      }


  }
}