package client.room

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestKit
import client.utils.MessageDictionary.{CreatePrivateRoom, CreatePublicRoom, GetJoinedRooms, JoinedRooms}
import client.CoreClient
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import common.TestConfig
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.GameServer
import server.room.ServerRoom
import akka.pattern.ask
import akka.util.Timeout
import common.http.Routes
import common.room.{Room, RoomJsonSupport, RoomProperty}
import server.utils.ExampleRooms
import server.utils.ExampleRooms.MyRoom

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.Try
import common.room.RoomPropertyValueConversions._

class ClientRoomSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with TestConfig
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with LazyLogging
  with RoomJsonSupport {

  private val ServerAddress = "localhost"
  private val ServerPort = CLIENT_ROOM_SPEC_SERVER_PORT

  private val RoomTypeName: String = "test_room"
  private val ServerLaunchAwaitTime = 10 seconds
  private val ServerShutdownAwaitTime = 10 seconds
  implicit private val DefaultTimeout: Timeout = 5 seconds
  implicit private val DefaultDuration: Duration = 5 seconds

  implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var gameServer: GameServer = _
  private var coreClient: ActorRef = _
  private var clientRoom: ClientRoom = _

  before {
    gameServer = GameServer(ServerAddress, ServerPort)
    gameServer.defineRoom(RoomTypeName, () => ServerRoom())
    gameServer.defineRoom(ExampleRooms.myRoomType, MyRoom)
    Await.ready(gameServer.start(), ServerLaunchAwaitTime)
    logger debug s"Server started at $ServerAddress:$ServerPort"
    coreClient = system actorOf CoreClient(Routes.httpUri(ServerAddress, ServerPort))
    val res = Await.result((coreClient ? CreatePublicRoom(RoomTypeName, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
    clientRoom = res.get
  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)
  }

  "A client room" must {
    "join and notify the core client" in {
      Await.result(clientRoom.join(), DefaultDuration)
      val res = Await.result( (coreClient ? GetJoinedRooms).mapTo[JoinedRooms], DefaultDuration).joinedRooms
      res should have size 1
    }

    "leave and notify the core client" in {
      Await.result(clientRoom.join(), DefaultDuration)
      Await.result(clientRoom.leave(), DefaultDuration)

      val res = Await.result( (coreClient ? GetJoinedRooms).mapTo[JoinedRooms], DefaultDuration).joinedRooms
      res should have size 0
    }

    "show the correct default room properties when properties are not overridden" in {
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room.properties should have size 3 // a, b, private
      room.properties should contain ("a", 0)
      room.properties should contain ("b", "abc")
    }

    "return correct room properties" in {
      val properties = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"))
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, properties)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room propertyOf "a" shouldEqual RoomProperty("a", 1)
      room propertyOf "b" shouldEqual RoomProperty("b", "qwe")
    }

    "show the correct room property values when properties are overridden" in {
      val properties = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"))
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, properties)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room.properties should have size 3 // a, b, private
      room.properties should contain ("a", 1)
      room.properties should contain ("b", "qwe")
      room.properties should contain (Room.roomPrivateStatePropertyName, false)
    }

    "return correct property values" in {
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room valueOf "a" shouldEqual 0
      room valueOf "b" shouldEqual "abc"
    }

    "have the private flag turned on when a private room is created" in {
      val res = Await.result((coreClient ? CreatePrivateRoom(ExampleRooms.myRoomType, Set.empty, "pwd")).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room valueOf Room.roomPrivateStatePropertyName shouldEqual true
    }

    "have the private flag turned off when a public room is created" in {
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room valueOf Room.roomPrivateStatePropertyName shouldEqual false
    }
  }
}
