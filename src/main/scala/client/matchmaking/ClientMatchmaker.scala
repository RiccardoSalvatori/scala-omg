package client.matchmaking

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import client.matchmaking.MatchmakingActor.{JoinMatchmaking, LeaveMatchmaking}
import client.room.{ClientRoom, JoinedRoom}
import common.communication.CommunicationProtocol.{MatchmakeTicket, SessionId}
import common.room.Room.{RoomId, RoomType}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Success}


trait ClientMatchmaker {

  private val DefaultMatchmakingTimeout = 5 minutes
  /**
   * Join the serverside matchmaking queue for this type of room
   *
   * @param roomType matchmaking room type to join
   * @param requestTimeout how much time to wait before failing, defualt is 5 minutes
   * @return a future that completes sucessfully when the matchmaker server side creates the match
   */
  def joinMatchmaking(roomType: RoomType, requestTimeout: FiniteDuration = DefaultMatchmakingTimeout): Future[JoinedRoom]

  /**
   * Leave the serverside matchmaking queue for this type of room serverside
   *
   * @param roomType matchmaking room room type to leave
   */
  def leaveMatchmaking(roomType: RoomType): Future[Any]

}

object ClientMatchmaker {

  def apply(coreClient: ActorRef, httpServerUri: String)
           (implicit system: ActorSystem): ClientMatchmaker = new ClientMatchmakerImpl(coreClient, httpServerUri)
}


class ClientMatchmakerImpl(private val coreClient: ActorRef,
                           private val httpServerUri: String)
                          (implicit val system: ActorSystem) extends ClientMatchmaker {

  private implicit val timeout: Timeout = 20 seconds
  private implicit val executor: ExecutionContextExecutor = system.dispatcher

  //keeps track of active matchmaking connections
  private var matchmakingConnections: Map[RoomType, ActorRef] = Map()
  private var promises: Map[RoomType, Promise[JoinedRoom]] = Map()

  override def joinMatchmaking(roomType: RoomType, requestTimeout: FiniteDuration): Future[JoinedRoom] = {
    this.matchmakingConnections.get(roomType) match {
      case Some(_) => // if already exists an active matchmaking connection for the room type, return that future
        promises(roomType).future
      case None =>
        val joinMatchmakingPromise = createPromise(roomType)
        val ref = system.actorOf(MatchmakingActor(roomType, this.httpServerUri))
        this.matchmakingConnections = this.matchmakingConnections.updated(roomType, ref)
        joinMatchmakingPromise.completeWith(createJoinMatchmakingFuture(ref, roomType, requestTimeout)).future
    }
  }

  override def leaveMatchmaking(roomType: RoomType): Future[Any] = {
    this.matchmakingConnections.get(roomType) match {
      case Some(ref) =>
        (ref ? LeaveMatchmaking).map { _ =>
          //make the join future fail
          this.promises(roomType).failure(new Exception("matchmaking left"))
          this.removeMatchmakeConnection(roomType)
        }
      case None =>
        //if there are no matchmaking connections for that room type just complete successfully
        Future.successful()
    }

  }

  /**
   * Creates a future that completes when matchmaking completes and the room is joined
   */
  private def createJoinMatchmakingFuture(matchmakingActor: ActorRef, roomType: RoomType, time: FiniteDuration): Future[JoinedRoom] = {
    matchmakingActor.ask(JoinMatchmaking)(Timeout.durationToTimeout(time)).flatMap {
      case Success(res) =>
        val ticket = res.asInstanceOf[MatchmakeTicket]
        val room = ClientRoom.createJoinable(coreClient, httpServerUri, ticket.roomId, Set())
        removeMatchmakeConnection(roomType)
        room.joinWithSessionId(ticket.sessionId)
      case Failure(ex) =>
        matchmakingActor ! LeaveMatchmaking
        matchmakingActor ! PoisonPill
        Future.failed(ex)
    }
  }

  private def removeMatchmakeConnection(roomType: RoomType) = {
    this.promises = this.promises - roomType
    this.matchmakingConnections(roomType) ! PoisonPill
    this.matchmakingConnections = this.matchmakingConnections - roomType
  }

  private def createPromise(roomType: RoomType): Promise[JoinedRoom] = {
    val p = Promise[JoinedRoom]()
    this.promises = this.promises.updated(roomType, p)
    p
  }
}




