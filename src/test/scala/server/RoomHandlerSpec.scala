package server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import common.room.SharedRoom.RoomId
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.room.{Client, ServerRoom}
import common.room.BasicRoomPropertyValueConversions._
import common.room.{FilterOptions, RoomProperty}
import org.scalatest.BeforeAndAfter
import server.routes.RouteCommonTestOptions

class RoomHandlerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with RouteCommonTestOptions with BeforeAndAfter {

  class MyRoom(override val roomId: RoomId) extends ServerRoom() {

    var a: Int = 1
    var b: String = "a"
    var c: Boolean = true

    override def onCreate(): Unit = {}
    override def onClose(): Unit = {}
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {}
  }
  class MyRoom2(override val roomId: RoomId) extends ServerRoom() {

    val a: Int = 1
    val b: String = "a"

    override def onCreate(): Unit = {}
    override def onClose(): Unit = {}
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {}
  }
  val myRoomType = "myRoomType"
  val myRoomType2 = "myRoomType2"

  private var roomHandler: RoomHandler = _

  before {
    roomHandler = RoomHandler()
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  behavior of "a RoomHandler"

  it should "start with no available rooms" in {
    this.roomHandler.getAvailableRooms() should have size 0
  }

  it should "create a new room on createRoom()" in {
    this.roomHandler.defineRoomType(TestRoomType, ServerRoom(_))
    this.roomHandler.createRoom(TestRoomType)
    this.roomHandler.getAvailableRooms() should have size 1
  }

  it should "return room of given type calling getRoomsByType() " in {
    val roomType1 = "type1"
    val roomType2 = "type2"
    this.roomHandler.defineRoomType(roomType1, ServerRoom(_))
    this.roomHandler.defineRoomType(roomType2, ServerRoom(_))
    this.roomHandler.createRoom(roomType1)
    this.roomHandler.createRoom(roomType2)
    this.roomHandler.createRoom(roomType2)

    this.roomHandler.getRoomsByType(roomType2) should have size 2
  }

  "An empty filter" should "not affect any room" in {
    roomHandler defineRoomType(myRoomType, id => new MyRoom(id))
    roomHandler createRoom myRoomType
    roomHandler.getAvailableRooms() should have size 1
    val filteredRooms = roomHandler.getAvailableRooms()
    filteredRooms should have size 1
  }

  "If no room can pass the filter, it" should "return an empty set" in {
    roomHandler defineRoomType(myRoomType, id => new MyRoom(id))
    roomHandler createRoom myRoomType
    val testProperty = RoomProperty("a", 0)
    val filter = FilterOptions just testProperty =:= 10
    val filteredRooms = roomHandler.getAvailableRooms(filter)
    filteredRooms should have size 0
  }

  "If a room does not contain a property specified in the filter, such room" should "not be inserted in the result" in {
    val testProperty = RoomProperty("c", true)
    val filter = FilterOptions just testProperty =:= true
    roomHandler defineRoomType(myRoomType, id => new MyRoom(id))
    roomHandler defineRoomType(myRoomType2, id => new MyRoom2(id))

    roomHandler createRoom myRoomType2
    val filteredRooms = roomHandler.getAvailableRooms(filter)
    roomHandler.getAvailableRooms() should have size 1
    filteredRooms should have size 0

    roomHandler createRoom myRoomType
    val filteredRooms2 = roomHandler.getAvailableRooms(filter)
    roomHandler.getAvailableRooms() should have size 2
    filteredRooms2 should have size 1
  }

  "Correct filter strategies" must "be applied to rooms' properties" in {
    val testProperty = RoomProperty("a", 1)
    val testProperty2 = RoomProperty("b", "a")
    val testProperty3 = RoomProperty("c", true)
    roomHandler defineRoomType(myRoomType, id => new MyRoom(id))
    roomHandler createRoom myRoomType

    val filter = FilterOptions just testProperty < 2 andThen testProperty2 =:= "a" andThen testProperty3 =!= false
    val filteredRooms = roomHandler.getAvailableRooms(filter)
    filteredRooms should have size 1

    val filter2 = FilterOptions just testProperty > 3
    val filteredRooms2 = roomHandler.getAvailableRooms(filter2)
    filteredRooms2 should have size 0
  }
}
