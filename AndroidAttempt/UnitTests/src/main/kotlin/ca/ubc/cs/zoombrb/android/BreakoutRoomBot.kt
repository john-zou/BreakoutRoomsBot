/*
 * Copyright (C) 2020 The University of British Columbia
 */
package ca.ubc.cs.zoombrb.android
import java.util.*

data class User(val userId: Long,
                val username: String)

data class TaggedUser(val userId: Long,
                      val username: String,
                      val tag: Tag)

/**
 * Given a list of User, return a list of TaggedUser, representing a breakout rooms assignment
 * @param users should be everyone in the meeting except for the Breakout Room Bot
 * @param idealNumStudentsPerRoom the ideal number of students per room, and will be used to distribute any users without a tag
 * @param renameCallback the rename function from the Zoom SDK (or a wrapper around it)
 */
fun getTaggedUsers(users: List<User>, idealNumStudentsPerRoom: Int, renameCallback: (userID: Long, newUserName: String) -> Unit): List<TaggedUser> {
    // Assign users that have tags
    val untaggedUsers: MutableList<User> = ArrayList()
    val taggedUsers: MutableList<TaggedUser> = ArrayList()

    val roomCounts = IntArray(MAX_ROOM_NUMBER + 1) { 0 }

    // Assign tagged users and update room counts
    users.forEach { user ->
        val parsedTag = TagParser.tryParseTag(user.username)
        if (parsedTag == null) {
            untaggedUsers.add(user)
        } else {
            taggedUsers.add(TaggedUser(user.userId, user.username, parsedTag))
            if (parsedTag.roomNumber != null) {
                ++roomCounts[parsedTag.roomNumber]
            }
        }
    }

    val emptyRooms: MutableList<Int> = ArrayList()
    val fullRooms: MutableList<Int> = ArrayList()
    val nonEmptyNonFullRooms: MutableList<Int> = ArrayList()

    for (i in 1..MAX_ROOM_NUMBER) {
        when {
            roomCounts[i] == 0 -> emptyRooms.add(i)
            roomCounts[i] >= idealNumStudentsPerRoom -> fullRooms.add(i)
            else -> nonEmptyNonFullRooms.add(i)
        }
    }

    // Assign untagged users to rooms, adding to priority queue of nonempty rooms
    val queue = PriorityQueue<Int> { room1, room2 -> roomCounts[room1] - roomCounts[room2] }
    queue.addAll(nonEmptyNonFullRooms)

    // Helper function
    fun tagUser(user: User, room: Int) {
        val newName = "($room) ${user.username}"
        renameCallback(user.userId, newName)
        taggedUsers.add(TaggedUser(user.userId, newName, Tag(room)))
        ++roomCounts[room]
    }

    while (untaggedUsers.isNotEmpty() && queue.isNotEmpty()) {
        val user = untaggedUsers.removeLast()
        val room = queue.poll()
        tagUser(user, room)

        // Add room back into minPQ if it's still under the desired room count
        if (roomCounts[room] < idealNumStudentsPerRoom) {
            queue.add(room)
        } else {
            fullRooms.add(room)
        }
    }

    // Add desiredRoomCount untagged users at a time to empty rooms
    while (untaggedUsers.size >= idealNumStudentsPerRoom && emptyRooms.isNotEmpty()) {
        val room = emptyRooms.removeLast()
        roomCounts[room] = idealNumStudentsPerRoom
        for (j in 1..idealNumStudentsPerRoom) {
            val user = untaggedUsers.removeLast()
            tagUser(user, room)
        }
        fullRooms.add(room)
    }

    // Make a minPQ out of the full rooms (which could have variable number of users) and add any remaining users there
    val fullRoomsQueue = PriorityQueue<Int> { room1, room2 -> roomCounts[room1] - roomCounts[room2] }
    fullRoomsQueue.addAll(fullRooms)
    while (untaggedUsers.isNotEmpty() && fullRoomsQueue.isNotEmpty()) {
        val room = fullRoomsQueue.poll()
        val user = untaggedUsers.removeLast()
        tagUser(user, room)
        fullRoomsQueue.add(room)
    }

    // Corner case: no user chose a numbered rooms, and there are < desiredCapacity users. Put them into room 1,
    // which is guaranteed to be empty
    val room = 1
    if (untaggedUsers.isNotEmpty()) {
        while (untaggedUsers.isNotEmpty()) {
            val user = untaggedUsers.removeLast()
            tagUser(user, room)
        }
        emptyRooms.remove(room) // for optional logging before return
        nonEmptyNonFullRooms.add(room) // for optional logging before return
    }

    return taggedUsers
}

