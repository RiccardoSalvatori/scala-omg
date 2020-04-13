package client.room

import akka.actor.{ActorRef, Props, Stash}
import client.utils.MessageDictionary._
import client.{BasicActor, HttpClient}
import common.room.Room.{RoomId, RoomPassword}
import common.communication.BinaryProtocolSerializer
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage}

import scala.util.{Failure, Success}

object ClientRoomActor {
  def apply[S](coreClient: ActorRef, serverUri: String, room: ClientRoom): Props =
    Props(classOf[ClientRoomActor[S]], coreClient, serverUri, room)
}

/**
 * Handles the connection with the server side room.
 * Notify the coreClient if the associated room is left or joined.
 */
case class ClientRoomActor[S](coreClient: ActorRef, httpServerUri: String, room: ClientRoom) extends BasicActor with Stash {
  private val httpClient = context.system actorOf HttpClient(httpServerUri)
  private var onMessageCallback: Option[Any => Unit] = None
  private var onStateChangedCallback: Option[Any => Unit] = None
  private var onCloseCallback: Option[() => Unit] = None

  private var joinPassword: RoomPassword = _

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitSocketResponse(replyTo: ActorRef, sessionId: Option[String]): Receive = onWaitSocketResponse(replyTo, sessionId) orElse fallbackReceive

  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = onSocketOpened(outRef, replyTo) orElse fallbackReceive

  def roomJoined(outRef: ActorRef): Receive = onRoomJoined(outRef) orElse fallbackReceive

  //actor states
  def onReceive: Receive = {
    case SendJoin(roomId: RoomId, sessionId: Option[String], password: RoomPassword) =>
      joinPassword = password
      httpClient ! HttpSocketRequest(roomId, BinaryProtocolSerializer)
      context.become(waitSocketResponse(sender, sessionId))

    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()

    case OnCloseCallback(callback) =>
      onCloseCallback = Some(callback)
      unstashAll()
  }

  def onWaitSocketResponse(replyTo: ActorRef, sessionId: Option[String]): Receive = {
    case RoomProtocolMessage => stash()

    case HttpSocketFail(code) =>
      replyTo ! Failure(new Exception(code.toString))
      context.become(receive)

    case HttpSocketSuccess(outRef) =>
      context.become(socketOpened(outRef, replyTo))
      unstashAll()
      val stringId: String = sessionId match {
        case Some(value) => value
        case None => "" //empty string is no id is specified
      }
      self ! SendProtocolMessage(RoomProtocolMessage(
        messageType = JoinRoom,
        sessionId = stringId,
        payload = joinPassword))

    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()

    case OnCloseCallback(callback) =>
      onCloseCallback = Some(callback)
      unstashAll()

  }

  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {

    case RoomProtocolMessage(ProtocolMessageType.JoinOk, sessionId, _) =>
      coreClient ! ClientRoomActorJoined
      replyTo ! Success(sessionId)
      context.become(roomJoined(outRef))

    case RoomProtocolMessage(ProtocolMessageType.RoomClosed, _, _) =>

    case RoomProtocolMessage(ProtocolMessageType.ClientNotAuthorized, _, _) =>
      replyTo ! Failure(new Exception("Can't join"))


    case SendProtocolMessage(msg) =>
      outRef ! msg

    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()

    case OnCloseCallback(callback) =>
      onCloseCallback = Some(callback)
      unstashAll()
  }


  def onRoomJoined(outRef: ActorRef): Receive = {

    case RoomProtocolMessage(ProtocolMessageType.ClientNotAuthorized, _, _) =>

    case RoomProtocolMessage(ProtocolMessageType.Tell, _, payload) => handleMessageReceived(payload)

    case RoomProtocolMessage(ProtocolMessageType.Broadcast, _, payload) => handleMessageReceived(payload)

    case RoomProtocolMessage(ProtocolMessageType.StateUpdate, _, payload) =>
      handleStateChangedReceived(payload)

    case RoomProtocolMessage(ProtocolMessageType.RoomClosed, _, _) =>
      this.onCloseCallback match {
        case Some(value) => value()
        case None => stash()
      }

    case SendLeave =>
      outRef ! RoomProtocolMessage(LeaveRoom)
      coreClient ! ClientRoomActorLeaved
      sender ! Success()
      context.become(receive)

    case SendStrictMessage(msg: Any with java.io.Serializable) =>
      self ! SendProtocolMessage(RoomProtocolMessage(MessageRoom, "", msg))

    case SendProtocolMessage(msg) =>
      outRef ! msg

    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()

    case OnCloseCallback(callback) =>
      onCloseCallback = Some(callback)
      unstashAll()

    case RetrieveClientRoom => sender ! ClientRoomResponse(this.room)
  }


  //private utilities

  private def handleMessageReceived(msg: Any): Unit = {
    //stash messages if callback is not defined
    //They will be handled as soon as the callback is defined
    handleIfDefinedOrStash(this.onMessageCallback, msg)

  }

  private def handleStateChangedReceived(state: Any): Unit = {
    //stash messages if callback is not defined
    //They will be handled as soon as the callback is defined
    handleIfDefinedOrStash(this.onStateChangedCallback, state)
  }


  private def handleIfDefinedOrStash(callback: Option[Any => Unit], msg: Any): Unit = {
    callback match {
      case Some(value) => value(msg)
      case None => stash()
    }
  }


}

