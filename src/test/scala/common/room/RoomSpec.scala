package common.room

import common.room.RoomPropertyValueConversions._
import common.room.Room.SharedRoom
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RoomSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter {

  var room: SharedRoom = _

  before {
    room = SharedRoom("id")
  }

  behavior of "Shared Room"

  it should "start with no properties at all" in {
    assert(room.sharedProperties.isEmpty)
  }

  it should "add the right property" in {
    for (i <- 0 until 10) {
      val property = RoomProperty(s"$i", i)
      room addSharedProperty property
      room.sharedProperties should have size (i + 1)
      assert(room.sharedProperties contains property)
    }
    room.sharedProperties should have size 10
  }
}
