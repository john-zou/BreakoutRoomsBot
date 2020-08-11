/*
 * Copyright (C) 2020 The University of British Columbia
 */
package ca.ubc.cs.zoombrb.android

import android.util.Log
import us.zoom.sdk.*
import java.util.*

data class User(val userId: Long,
                var username: String)

data class TaggedUser(val userId: Long,
                      val username: String,
                      val tag: Tag)

data class Tag(val roomNumber: Int? = null,
               val isLoneWolf: Boolean = false,
               val isProf: Boolean = false,
               val isTA: Boolean = false)

data class CreatedBreakoutRooms(val instructorsRoom: String, val loneWolvesRoom: String, val numberedRooms: Map<Int, String>)

/**
 * The Breakout Room Bot API. Singleton (Kotlin object; seen by Java as enum singleton)
 *
 *  From Java, this should be used as an enum singleton:
 * `BreakoutRoomBot.INSTANCE.somePublicFunction()`
 *
 * `BreakoutRoomBot::respondToChatMessage` is the single hook required for chat based API
 */
object BreakoutRoomBot {
    const val MAX_ROOM_NUMBER = 48
    const val TA_TAG = "TA"
    const val PROF_TAG = "Prof"

    private const val DEFAULT_IDEAL_NUM_STUDENTS_PER_ROOM = 5
    private const val INSTRUCTORS_ROOM_NAME = "Instructors"
    private const val LONE_WOLVES_ROOM_NAME = "Lone Wolves"
    private const val PASSWORD = "ubc" // Unused
    private const val HELP_TRUSTED = "Here is a list of commands: \n\n" +
            "\n" +
            "  !setup -- create 50 breakout rooms -- only needs to be done once\n" +
            "\n" +
            "  !start -- start a breakout session\n" +
            "\n" +
            "  !end -- end a breakout session\n" +
            "\n" +
            "  !assignall -- assign everyone to breakout rooms and add tags. Note: this happens automatically with the !start command\n" +
            "\n" +
            "  !assign <name> <roomnumber> -- assign someone to a room manually\n" +
            "\n" +
            "  !host -- make you the host (emergency)"
    private const val HELP_GENERAL = "To join a specific breakout room, private message me:\n" +
            "  !room <number> (e.g. '!room 5')\n" +
            "\n" +
            "To access instructor-only commands: \n" +
            "  !trustme <password>"
    private const val TRUST_ME_FIRST = "'!trustme <password>' first"

    private var createdBreakoutRooms: CreatedBreakoutRooms? = null
    private val tagParser = TagParser
    private var idealNumStudentsPerRoom = DEFAULT_IDEAL_NUM_STUDENTS_PER_ROOM // TODO: expose API to modify this

    /**
     * The Zoom Chat hook. The starting point for processing all Zoom Meeting chat commands.
     */
    fun respondToChatMessage(msg: InMeetingChatMessage) {
        when {
            msg.isChatToAll -> respondToPublicChatMessage(msg)
            ZoomSDK.getInstance().inMeetingService.isMyself(msg.senderUserId) -> Log.v("BRB", "self message")
            else -> respondToPrivateChatMessage(msg)
        }
    }

    // Helper functions for SDK APIs
    private fun inMeetingService(): InMeetingService  = ZoomSDK.getInstance().inMeetingService
    private fun boController(): InMeetingBOController = ZoomSDK.getInstance().inMeetingService.inMeetingBOController
    private fun boData(): IBOData = boController().boDataHelper
    private fun boCreator(): IBOCreator = boController().boCreatorHelper
    private fun boAdmin(): IBOAdmin = boController().boAdminHelper

    /**
     * Creates the Instructor, Lone Wolves, and 48 numbered rooms (named Breakout Room 1, etc.), if
     * they have not been created yet
     *
     * Called by assignBreakoutRooms
     */
    fun createBreakoutRooms() {
        Log.v("BRB", "createBreakoutRooms()")

        if (createdBreakoutRooms != null) {
            Log.v("BRB", "createBreakoutRooms() returning as they have been created already")
            return
        }

        var anyRoomFailedToCreate = false
        val creator = boCreator()
        var instructorsRoom: String? = null
        var loneWolvesRoom: String? = null
        val numberedRooms = HashMap<Int, String>()

        // Helper functions
        fun createInstructorsRoom() {
            val instructorsRoomID = creator.createBO(INSTRUCTORS_ROOM_NAME)

            if (instructorsRoomID.isNullOrEmpty()) {
                Log.e("BRB", "createBO for instructor room failed")
                anyRoomFailedToCreate = true
            } else {
                instructorsRoom = instructorsRoomID
            }
            Log.v("BRB", "createBO returned: $instructorsRoomID")
        }

        fun createNumberedBreakoutRooms(from: Int, toInclusive: Int) {
            for (i in from..toInclusive) {
                val name = "Breakout Room $i"
                val numberedRoomID = creator.createBO(name)
                if (numberedRoomID.isNullOrEmpty()) {
                    Log.e("BRB", "createBO for Breakout Room $i failed")
                    anyRoomFailedToCreate = true
                } else {
                    numberedRooms[i] = numberedRoomID
                }
                Log.v("BRB", "createBO returned: $numberedRoomID")
            }
        }

        fun createLoneWolvesRoom() {
            val loneWolvesRoomID = creator.createBO(LONE_WOLVES_ROOM_NAME)
            if (loneWolvesRoomID.isNullOrEmpty()) {
                Log.e("BRB", "createBO for lone wolves room failed")
                anyRoomFailedToCreate = true
            } else {
                loneWolvesRoom = loneWolvesRoomID
            }
            Log.v("BRB", "createBO returned: $loneWolvesRoomID")
        }


        createInstructorsRoom()
        createNumberedBreakoutRooms(1, MAX_ROOM_NUMBER)
        createLoneWolvesRoom()

        Log.v("BRB", "createBreakoutRooms() complete. anyRoomFailedToCreate: $anyRoomFailedToCreate")
        if (anyRoomFailedToCreate) {
            Log.e("BRB", "1+ of the room creations failed.")
            Log.e("BRB", "instructors room: $instructorsRoom")
            Log.e("BRB", "love wolves room: $loneWolvesRoom")
            Log.e("BRB", "numbered rooms: $numberedRooms")

        } else {
            this.createdBreakoutRooms = CreatedBreakoutRooms(instructorsRoom!!, loneWolvesRoom!!, numberedRooms)
        }
    }

    private fun clearBreakoutRooms() {
        Log.v("BRB", "clearBreakoutRooms()")
        val creator = boCreator()
        val data = boData()
        val currentBreakoutRooms = boData().boMeetingIDList
        currentBreakoutRooms.forEach { boID ->
            Log.v("BRB", "Getting BO by ID: $boID")
            val bo = data.getBOMeetingByID(boID)
            bo.boUserList.forEach { userID ->
                creator.removeUserFromBO(userID, boID)
            }
        }
    }

    fun renameUser(userId: Long, newName: String) {
        ZoomSDK.getInstance().inMeetingService.changeName(newName, userId)
    }

    /**
     * Given a list of User, return a list of TaggedUser, representing a breakout rooms assignment.
     * `renameCallback` is called for each of the `users` that doesn't have a `username` with a `Tag`
     *
     * @param users should be everyone in the meeting except for the Breakout Room Bot
     * @param idealNumStudentsPerRoom the ideal number of students per room, and will be used to distribute any users without a tag
     * @param renameCallback the rename function from the Zoom SDK (or a wrapper around it)
     *
     * Tested in BRBTest project (file: BreakoutRoomBotKtTest)
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

    fun makeUserCoHost(userId: Long) {
        val sdkErr = inMeetingService().assignCohost(userId)
        Log.v("BRB", "...SDKErr: $sdkErr")
    }

    /**
     * Assigns breakout rooms based on
     * - User tags
     * - Room size guideline (desiredCapacity)
     *
     * Pre-conditions:
     * - BRB is host
     * - Trusted users have Tag with isTrusted = true
     * - Breakout Rooms are enabled for the meeting
     * - Breakout Rooms are not in session (not sure whether this matters)
     * - There is an instructors room, MAX_ROOM_NUMBER regular rooms, and lone wolves room setup and empty
     * - All users are unassigned
     * - No more than 200 students have the same tag (unchecked assumption) TODO: handle this very rare case
     *
     * Post-conditions:
     * - Every student who had a tag is assigned to the room of their choice
     * - Students who didn't have a tag are assigned to existing rooms in a manner that tries to
     * achieve desiredCapacity for each room and also that students are not left alone if they didn't
     * have a tag
     * - Every trusted user is assigned to the instructors' room
     * - Every lone wolf is assigned to the lone wolves' room
     *    - No student without 0 or 00 tag is assigned lone wolf by BRB
     */
    fun assignBreakoutRooms() {
        Log.v("BRB", "assignBreakoutRooms()")

        createBreakoutRooms()
        clearBreakoutRooms()

        val users = inMeetingService().inMeetingUserList.map { userId ->
            User(userId, inMeetingService().getUserInfoById(userId).userName)
        }

        Log.v("BRB getTaggedUsers input (users)", users.toString())

        val taggedUsers = getTaggedUsers(users, idealNumStudentsPerRoom, ::renameUser)

        Log.v("BRB getTaggedUsers output (taggedUsers)", taggedUsers.toString())
        taggedUsers.forEach { taggedUser ->
            if (taggedUser.tag.isProf || taggedUser.tag.isLoneWolf) {
                Log.v("BRB", "Making ${taggedUser.username} a CoHost.")
                makeUserCoHost(taggedUser.userId)
            }
        }

        val breakoutRoomUsers = boData().unassginedUserList

        breakoutRoomUsers.forEach { sid ->
            val username = boData().getBOUserName(sid)
            val tag = tagParser.tryParseTag(username)
            if (tag == null) {
                Log.e("BRB", "Phase 2 -- User with SID [$sid] and username [$username] does NOT have a tag!")
            } else {
                Log.v("BRB", "Phase 2 -- User with SID [$sid] and username [$username] has tag: [$tag]")
                when {
                    tag.isTA -> boCreator().assignUserToBO(sid, createdBreakoutRooms!!.instructorsRoom)
                    tag.isProf -> boCreator().assignUserToBO(sid, createdBreakoutRooms!!.instructorsRoom)
                    tag.isLoneWolf -> boCreator().assignUserToBO(sid, createdBreakoutRooms!!.loneWolvesRoom)
                    tag.roomNumber != null -> boCreator().assignUserToBO(sid, createdBreakoutRooms!!.numberedRooms[tag.roomNumber])
                    else -> Log.e("BRB","Error when assigning room: invalid tag!")
                }
            }
        }
    }

    fun startBreakoutRooms() {
        Log.i("BRB", "startBreakoutRooms()")
        try {
            assignBreakoutRooms() }
        catch (err: Exception) {
            Log.e("BRB", err.toString())
        }
        // val result = boAdmin().startBO()
        // Log.d("BRB", "BOAdminHelper::startBO returned: $result")
    }

    fun endBreakoutRooms() {
        Log.i("BRB", "endBreakoutRooms")
        val result = boAdmin().stopBO()
        Log.d("BRB", "BOAdminHelper::stopBO returned: $result")
    }

    /**
     * Make all commands available to the user and makes them Co-Host.
     * https://support.zoom.us/hc/en-us/articles/201362603-Host-and-co-host-controls-in-a-meeting
     */
//    fun trustUser(userId: Long) {
//        Log.v("BRB", "trustUser")
//        trustedUserIds.add(userId)
//
//        // helper
//        fun giveTrustedTagToUser(): String {
//            var username = inMeetingService().getUserInfoById(userId).userName
//            val tag = tagParser.tryParseTag(username)
//            if (tag == null || !tag.isTrusted) {
//                username = "($TRUSTED_TAG) $username"
//                renameUser(userId, username)
//            }
//            return username
//        }
//
//        if (this.users[userId] == null) {
//            val username = giveTrustedTagToUser()
//            this.users[userId] = User(userId, username, tag = Tag(isTrusted = true))
//        } else {
//            val user = this.users[userId]!!
//            if (user.tag == null) {
//                giveTrustedTagToUser()
//            }
//        }
//
//        if (ZoomSDK.getInstance().inMeetingService.isMeetingHost) {
//            val err = ZoomSDK.getInstance().inMeetingService.assignCohost(userId)
//            if (err == MobileRTCSDKError.SDKERR_SUCCESS) {
//                sendPrivateMessage(userId, "You are now a co-host. For a list of commands, send me '!help'", "trustUser")
//            } else {
//                sendPrivateMessage(userId, "There was an unexpected error when making you a co-host, but you have access to all commands now.", "trustUser")
//            }
//        } else {
//            sendPrivateMessage(userId, "For a list of commands, send me '!help'. Note: I am not currently the host.", "trustUser not host")
//        }
//    }

    /**
     * Hands over host status to a user
     */
    fun makeHost(userId: Long) {
        if (ZoomSDK.getInstance().inMeetingService.isMeetingHost) {
            val sdkErr = ZoomSDK.getInstance().inMeetingService.makeHost(userId)
            Log.d("BRB", "makeHost sdkErr = $sdkErr")
            if (sdkErr == MobileRTCSDKError.SDKERR_SUCCESS) {
                sendPrivateMessage(userId, "You are now the host.", "makeHost")
            } else {
                sendPrivateMessage(userId, "Unexpected error!", "makeHost, unexpected error")
            }
        } else {
            sendPrivateMessage(userId, "I cannot make you the host as I am not the host.", "makeHost, bot is not host")
        }
    }

    fun informUserOfHelpCommand(userId: Long) {
        sendPrivateMessage(userId, "I could not understand you. '!help' to see a list of commands.", "informUserOfHelpCommand")
    }

    private fun userIsTrusted(userId: Long): Boolean {
        val userInfo = inMeetingService().getUserInfoById(userId)
        return if (userInfo == null) {
            Log.e("BRB", "userIsTrusted($userId) .. userInfo is null")
            false
        } else {
            val tag = tagParser.tryParseTag(userInfo.userName)
            tag != null && (tag.isProf || tag.isTA)
        }
    }

    fun respondToHelpCommand(userId: Long) {
        if (userIsTrusted(userId)) {
            sendPrivateMessage(userId, HELP_TRUSTED, "respondToHelpCommand, trusted")
        } else {
            sendPrivateMessage(userId, HELP_GENERAL, "respondToHelpCommand, general")
        }
    }

    private fun sendPrivateMessage(userId: Long, msg: String, logMsg: String = "sendPrivateMessage") {
        Log.v("BRB", "$logMsg, userId = $userId")
        val sdkErr = ZoomSDK.getInstance().inMeetingService.inMeetingChatController.sendChatToUser(userId, msg)
        Log.i("BRB", "sdkErr: $sdkErr")
    }

//    // For development
//    private fun test1() {
//        // Does a user become unassigned if they were previously assigned and we remove the breakout room?
//        setupBreakoutRooms()
//        val unassignedUsers = boData().unassginedUserList
//        Log.e("BRB TEST1", "unassigned users size: ${unassignedUsers.size}")
//        // assign them all to instructors room
//        unassignedUsers.forEach{ Log.e("BRB", "assigning returned: ${boCreator().assignUserToBO(it, instructorsRoom!!.boID)}") }
//        val unassignedUsersAfterAssigning = boData().unassginedUserList
//        Log.e("BRB TEST1", "unassigned users size after assigning to instructors room: ${unassignedUsersAfterAssigning.size}")
//        Log.e("BRB TEST1", "instructors room: ${instructorsRoom!!.boID}")
//        Log.e("BRB TEST1", "all users assigned to instructors room")
//    }
//
//    private fun test2() {
//        // Remove users from breakout rooms
//        clearBreakoutRooms()
//        val unassignedUsers = boData().unassginedUserList
//        Log.e("BRB TEST1", "unassigned users size (expecting ZERO): ${unassignedUsers.size}")
//    }

    /**
     * Responds to all private chat messages
     */
    private fun respondToPrivateChatMessage(msg: InMeetingChatMessage) {
        Log.v("BRB", "respondToPrivateChatMessage")
        val split = split(msg.content)
        if (split.isEmpty()) {
            // Silently ignore
            Log.v("BRB", "Got an empty private message from a user, ignoring.")
        } else {
            val command = split[0].toLowerCase(Locale.getDefault())
            val argCount = split.size - 1
            when (command) {
                "!help" -> respondToHelpCommand(msg.senderUserId)
                "!host", "!hostme" -> respondToHostCommand(msg.senderUserId)
//                "!trust", "!trustme", "!pw" -> if (argCount == 1) {
//                    respondToTrustCommand(msg.senderUserId, split[1])
//                } else {
//                    respondToTrustCommandInvalidNumberOfArguments(msg.senderUserId)
//                }
                "!test" -> boAdmin().startBO()
//                "!test2" -> test2() // development
                "!start", "!startbo" -> respondToStartBreakoutRoomsCommand(msg.senderUserId)
                "!end", "!endbo" -> respondToEndBreakoutRoomsCommand(msg.senderUserId)
                "!setupbo", "!setup" -> respondToSetupCommand(msg.senderUserId)
                "!assignall" -> respondToAssignAllBreakoutRoomsCommand(msg.senderUserId)
                "!assign" -> if (argCount == 2) {
//                        respondToAssignCommand(msg.getSenderUserId(), split[1], split[2]);
                } else {
//                        respondToAssignCommandInvalidNumberOfArguments(msg.getSenderUserId());
                }
                "!room" ->                     // Workflow: student enters
                    if (argCount == 1) {
//                        respondToRoomCommand(msg.getSenderUserId(), split[1]);
                    } else {
//                        respondToRoomCommandInvalidNumberOfArguments(msg.getSenderUserId());
                    }
                else -> informUserOfHelpCommand(msg.senderUserId)
            }
        }
    }

    private fun respondToAssignAllBreakoutRoomsCommand(senderUserId: Long) {
        if (!userIsTrusted(senderUserId)) {
            sendPrivateMessage(senderUserId, TRUST_ME_FIRST, "respondToAssignAllBreakoutRoomsCommand, not trusted")
        } else if (!ZoomSDK.getInstance().inMeetingService.isMeetingHost) {
            sendPrivateMessage(senderUserId, "I need to have host status to do that.", "attempt to assign all BO, BRB not host")
        } else {
            sendPrivateMessage(senderUserId, "Assigning users to their breakout rooms.", "respondToAssignAllBreakoutRoomsCommand")
            assignBreakoutRooms()
        }
    }

    private fun respondToSetupCommand(senderUserId: Long) {
        if (!userIsTrusted(senderUserId)) {
            sendPrivateMessage(senderUserId, TRUST_ME_FIRST, "respondToSetupCommand not trusted")
        } else if (!ZoomSDK.getInstance().inMeetingService.isMeetingHost) {
            sendPrivateMessage(senderUserId, "I need to have host status to do that.", "attempt to setup BO, BRB not host")
        } else {
            sendPrivateMessage(senderUserId, "Setting up 50 breakout rooms.", "respondToSetupCommand")
            createBreakoutRooms()
        }
    }

    private fun respondToEndBreakoutRoomsCommand(senderUserId: Long) {
        if (!userIsTrusted(senderUserId)) {
            sendPrivateMessage(senderUserId, TRUST_ME_FIRST, "End BO chat command")
        } else if (!ZoomSDK.getInstance().inMeetingService.isMeetingHost) {
            sendPrivateMessage(senderUserId, "I need to have host status to do that.", "attempt to end BO, BRB not host")
        } else if (!ZoomSDK.getInstance().inMeetingService.inMeetingBOController.isBOStarted) {
            sendPrivateMessage(senderUserId, "There doesn't appear to be a breakout rooms session to end.", "attempt to end non-started BO")
        } else {
            sendPrivateMessage(senderUserId, "Stopping the breakout rooms session.", "end breakout rooms session")
            endBreakoutRooms()
        }
    }

    private fun respondToStartBreakoutRoomsCommand(senderUserId: Long) {
        if (!userIsTrusted(senderUserId)) {
            sendPrivateMessage(senderUserId, TRUST_ME_FIRST, "Start BO chat command")
        } else if (!ZoomSDK.getInstance().inMeetingService.isMeetingHost) {
            sendPrivateMessage(senderUserId, "I need to have host status to do that.", "attempt to start BO, BRB not host")
        } else if (ZoomSDK.getInstance().inMeetingService.inMeetingBOController.isBOStarted) {
            sendPrivateMessage(senderUserId, "It appears breakout rooms are currently in session", "attempt to start started BO")
        } else {
            sendPrivateMessage(senderUserId, "Starting breakout rooms session.", "start breakout rooms session")
            startBreakoutRooms()
        }
    }

    private fun respondToTrustCommandInvalidNumberOfArguments(senderUserId: Long) {
        sendPrivateMessage(senderUserId, "The 'trust' command must be followed by exactly 1 argument, the password.", "respondToTrustCommand invalid # of args")
    }

//    private fun respondToTrustCommand(senderUserId: Long, pw: String) {
//        if (pw == PASSWORD) {
//            trustUser(senderUserId)
//        } else {
//            sendPrivateMessage(senderUserId, "The password was not accepted. Note: it is CaSe-SeNsItiVe.", "respondToTrustCommand wrong pw")
//        }
//    }

    private fun respondToHostCommand(senderUserId: Long) {
        if (userIsTrusted(senderUserId)) {
            makeHost(senderUserId)
        } else {
            sendPrivateMessage(senderUserId, TRUST_ME_FIRST, "respondToHostCommand, not trusted")
        }
    }

    private fun respondToPublicChatMessage(msg: InMeetingChatMessage) {
        Log.v("BRB", "respondToPublicChatMessage (doing nothing)")
        // Does not respond to public chat right now
    }

    /**
     * Helper method, splits the string on whitespace, treating multiple whitespace as a single whitespace
     */
    private fun split(str: String): Array<String> {
        return str.trim { it <= ' ' }.split("\\s+".toRegex()).toTypedArray()
    }

}