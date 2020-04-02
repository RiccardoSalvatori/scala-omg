package client.utils

import client.room.ClientRoom.ClientRoom
import common.FilterOptions
import common.SharedRoom.{RoomId, RoomType}

object MessageDictionary {

  case class CreatePublicRoom(roomType: RoomType, roomOption: Any)

  case class JoinOrCreate(roomType: RoomType, roomOption: Any)

  case class Join(roomType: RoomType, roomOption: Any)

  case class JoinById(roomType: RoomId)

  case class GetAvailableRooms(roomType: RoomType)

  case class GetFilteredAvailableRooms(filterOptions: FilterOptions)

  case class NewJoinedRoom(roomId: ClientRoom)

  case class GetJoinedRooms()

  case class JoinedRooms(rooms: Set[ClientRoom])

  case class UnknownMessageReply()
}
