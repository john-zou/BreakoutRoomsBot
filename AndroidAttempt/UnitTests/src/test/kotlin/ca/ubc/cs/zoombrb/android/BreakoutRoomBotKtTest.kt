/*
 * Copyright (C) 2020 The University of British Columbia
 */
package ca.ubc.cs.zoombrb.android

import org.junit.jupiter.api.Test
import kotlin.test.expect

internal class BreakoutRoomBotKtTest {
    @Test
    fun `getTaggedUsers empty input`() {
        expect(0) { getTaggedUsers(ArrayList(), 5) { _, _ -> }.size }
    }

    @Test
    fun `getTaggedUsers single user with tag in name`() {
        val userId = 12345L
        val username = "(1) User Name"

        expect(listOf(TaggedUser(userId, username, Tag(1)))) {
            getTaggedUsers(listOf(User(userId, username)), 5) { _, _ -> }
        }
    }

    @Test
    fun `getTaggedUsers single user without tag in name`() {
        val userId = 12345L
        val username = "User Name"
        expect(listOf(TaggedUser(userId, "(1) User Name", Tag(1)))) {
            getTaggedUsers(listOf(User(userId, username)), 5) { _, _ -> }
        }
    }

    @Test
    fun `getTaggedUsers large example`() {
        val idealRoomSize = 4

        // Some TAs, Prof, and Lone Wolves
        val prof = User(1000, "(Prof) prof")
        val ta1 = User(1001, "(TA) teaching assistant1")
        val ta2 = User(1002, "(TA) teaching assistant2")
        val ta3 = User(1003, "(TA) teaching assistant3")
        val ta4 = User(1004, "(TA) teaching assistant4")
        val ta5 = User(1005, "(TA) teaching assistant5")
        val lw1 = User(1011, "(00) lone wolf1")
        val lw2 = User(1012, "(00) lone wolf2")
        val lw3 = User(1013, "(00) lone wolf3")
        val lw4 = User(1014, "(00) lone wolf4")
        val lw5 = User(1015, "(00) lone wolf5")
        val lw6 = User(1016, "(00) lone wolf6")
        val lw7 = User(1017, "(00) lone wolf7")
        val lw8 = User(1018, "(00) lone wolf8")

        // Room 10: missing 3
        // Room 11: missing 2
        // Room 12: missing 1
        // Room 13: perfectly full
        // Room 15, 16: over full
        // All other rooms empty
        val u1 = User(11, "(10) user name1")
        val u2 = User(12, "(11) user name2")
        val u3 = User(13, "(11) user name3")
        val u4 = User(14, "(12) user name4")
        val u5 = User(15, "(12) user name5")
        val u6 = User(16, "(12) user name6")
        val u7 = User(17, "(13) user name7")
        val u8 = User(18, "(13) user name8")
        val u9 = User(19, "(13) user name9")
        val u10 = User(20, "(13) user name10")
        val u11 = User(21, "(15) user name11")
        val u12 = User(22, "(15) user name12")
        val u13 = User(23, "(15) user name13")
        val u14 = User(24, "(15) user name14")
        val u15 = User(25, "(15) user name15")
        val u16 = User(26, "(16) user name16")
        val u17 = User(27, "(16) user name17")
        val u18 = User(28, "(16) user name18")
        val u19 = User(29, "(16) user name19")
        val u20 = User(30, "(16) user name20")
        val u21 = User(31, "(16) user name21")
        val u22 = User(32, "user name22") // filling nonEmptyNonFullRooms
        val u23 = User(33, "user name23") // filling nonEmptyNonFullRooms
        val u24 = User(34, "user name24") // filling nonEmptyNonFullRooms
        val u25 = User(35, "user name25") // filling nonEmptyNonFullRooms
        val u26 = User(36, "user name26") // filling nonEmptyNonFullRooms
        val u27 = User(37, "user name27") // filling nonEmptyNonFullRooms
        val u28 = User(38, "user name28") // going to an empty room
        val u29 = User(39, "user name29") // going to an empty room
        val u30 = User(40, "user name30") // going to an empty room
        val u31 = User(41, "user name31") // going to an empty room
        val u32 = User(42, "user name32") // going to a full room
        val u33 = User(43, "user name33") // going to a full room
        val u34 = User(44, "user name34") // going to a full room

        val input = listOf(prof, ta1, ta2, ta3, ta4, ta5,
                lw1, lw2, lw3, lw4, lw5, lw6, lw7, lw8,
                u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15, u16, u17, u18, u19, u20,
                u21, u22, u23, u24, u25, u26, u27, u28, u29, u30, u31, u32, u33, u34)

        val expectedPreassignedOutputs = setOf(
                // Prof, TAs, lone wolves
                TaggedUser(prof.userId, prof.username, Tag(isProf = true)),
                TaggedUser(ta1.userId, ta1.username, Tag(isTA = true)),
                TaggedUser(ta2.userId, ta2.username, Tag(isTA = true)),
                TaggedUser(ta3.userId, ta3.username, Tag(isTA = true)),
                TaggedUser(ta4.userId, ta4.username, Tag(isTA = true)),
                TaggedUser(ta5.userId, ta5.username, Tag(isTA = true)),
                TaggedUser(lw1.userId, lw1.username, Tag(isLoneWolf = true)),
                TaggedUser(lw2.userId, lw2.username, Tag(isLoneWolf = true)),
                TaggedUser(lw3.userId, lw3.username, Tag(isLoneWolf = true)),
                TaggedUser(lw4.userId, lw4.username, Tag(isLoneWolf = true)),
                TaggedUser(lw5.userId, lw5.username, Tag(isLoneWolf = true)),
                TaggedUser(lw6.userId, lw6.username, Tag(isLoneWolf = true)),
                TaggedUser(lw7.userId, lw7.username, Tag(isLoneWolf = true)),
                TaggedUser(lw8.userId, lw8.username, Tag(isLoneWolf = true)),
                // Pre-tagged numbered room users (u1 to 21)
                TaggedUser(u1.userId, u1.username, Tag(10)),
                TaggedUser(u2.userId, u2.username, Tag(11)),
                TaggedUser(u3.userId, u3.username, Tag(11)),
                TaggedUser(u4.userId, u4.username, Tag(12)),
                TaggedUser(u5.userId, u5.username, Tag(12)),
                TaggedUser(u6.userId, u6.username, Tag(12)),
                TaggedUser(u7.userId, u7.username, Tag(13)),
                TaggedUser(u8.userId, u8.username, Tag(13)),
                TaggedUser(u9.userId, u9.username, Tag(13)),
                TaggedUser(u10.userId, u10.username, Tag(13)),
                TaggedUser(u11.userId, u11.username, Tag(15)),
                TaggedUser(u12.userId, u12.username, Tag(15)),
                TaggedUser(u13.userId, u13.username, Tag(15)),
                TaggedUser(u14.userId, u14.username, Tag(15)),
                TaggedUser(u15.userId, u15.username, Tag(15)),
                TaggedUser(u16.userId, u16.username, Tag(16)),
                TaggedUser(u17.userId, u17.username, Tag(16)),
                TaggedUser(u18.userId, u18.username, Tag(16)),
                TaggedUser(u19.userId, u19.username, Tag(16)),
                TaggedUser(u20.userId, u20.username, Tag(16)),
                TaggedUser(u21.userId, u21.username, Tag(16)),
        )

        val renamedUsers: MutableMap<Long, String> = HashMap()
        fun rename(userId: Long, username: String) {
            renamedUsers[userId] = username
        }

        val output = getTaggedUsers(input, idealRoomSize, ::rename)
        expect(true) {
            output.containsAll(expectedPreassignedOutputs)
        }
        expect(true) { output.size == input.size }

        // tally final room counts and ensure they are optimal
        val roomCounts = IntArray(MAX_ROOM_NUMBER + 1) { 0 }
        var extraRoom: Int? = null
        output.forEach { taggedUser ->
            val roomNumber = taggedUser.tag.roomNumber
            if (roomNumber != null) {
                if (roomNumber !in 10..13 && roomNumber != 15 && roomNumber != 16) {
                    extraRoom = roomNumber
                }
                ++roomCounts[roomNumber]
            }
        }

        assert(roomCounts[10] in 4..5)
        expect(true) { roomCounts[11] in 4..5 }
        expect(true) { roomCounts[12] in 4..5 }
        expect(true) { roomCounts[13] in 4..5 }
        expect(true) { roomCounts[15] == 5 }
        expect(true) { roomCounts[16] == 6 }
        expect(true) { roomCounts[extraRoom!!] in 4..5 }

        // check that user IDs 32..44 got renamed properly
        assert(renamedUsers.size == 13)
        val idToExpectedNewUsername = HashMap<Long, String>()
        output.forEach { taggedUser ->
            if (taggedUser.userId in 32..44) {
                expect("(${taggedUser.tag.roomNumber!!}) user name${taggedUser.userId - 10}") { taggedUser.username }
                idToExpectedNewUsername[taggedUser.userId] = taggedUser.username
            }
        }
        for ((id, expectedRename) in idToExpectedNewUsername) {
            expect(expectedRename){ renamedUsers[id] }
        }
    }
}
