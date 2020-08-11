# Introduction

- This is some analysis of the logs / events gathered on Fri Aug 7
- There are two log files, one from a small test meeting, and one from the CPSC 320 lecture

# Log File 1. Small Meeting

## Participants: 4
1. BRB (Breakout Room Bot) (Android App on Emulator, using Zoom Android SDK, on John's computer)
2. Mourud (regular Zoom client, on his own computer)
3. John #1, (regular Zoom client, same computer as BRB)
4. John #2, (regular Zoom client, different computer)

### Test 1: Mourud and I tested the ability of Breakout Room Bot to correctly parse a tag, set up breakout rooms, and assign the correct users to them.

- Mourud had the instructor tag, John #1 had tag 3, John #2 had tag 5.

## Sequence of events from log
- Host status is given to BRB by John #1 (who started the meeting, note: Non-UBC account)
- Mourud receives trusted tag through private chat interface ("!trustme p@ssword")
    - From BRB log: Mourud's ID is 16781312. The User ID is not permanent.
- Mourud used "assign all breakout rooms" command, which automatically creates breakout rooms if they don't exist
    - From BRB log: 50 breakout rooms were successfully created
    - John #1's tag was correctly determined to be "3"
    - John #2's tag was correctly determined to be "5"
    - Mourud's tag was correctly determined to be a "trusted" tag, meaning he will be assigned to the Instructors' room
    - BRB successfully finishes assigning everyone to breakout rooms
        - Mourud has Breakout Room UserID 7a2e0cf3efc0070246ee6f3b3f7ca1a1
        - John #1 has Breakout Room UserID c7e457c40854978c97facaf740d4021d
        - John #2 has Breakout Room UserID f54eb02b185a224748bda5f20c5f2faa
- Mourud commands BRB to make him the host, in order to see the breakout room assignments. **However, they were not visible to him!**
- Mourud passes host back to BRB
- Mourud issues "assign all breakout rooms" command again
    - BRB did not create new rooms, but did the same assignment as before
        - Mourud has Breakout Room UserID 7a2e0cf3efc0070246ee6f3b3f7ca1a1, this did not change
        - John #1 has Breakout Room UserID c7e457c40854978c97facaf740d4021d, this did not change
        - John #2 has Breakout Room UserID f54eb02b185a224748bda5f20c5f2faa, this did not change
- Mourud commanded BRB to start breakout rooms
    - Mourud, John #1 and John #2 all got popup invitation to join the correct breakout room
    - The breakout room works as normal

### Test 2: Mourud and I tested the ability for BRB to handle users without tags in their username
Note: the breakout room bot was not reset after the previous test. This might have caused unexpected behavior with regards to the Zoom SDK as noted later.

- According to the log, Mourud's UserID is now 16778240. This was John #1's old ID!
    - John #1 has UserID 16779264. This was John #2's old ID!
    - John #2 has UserID 16781312, and this is Mourud's previous ID!
    - (Aug 10) Upon further searching, [is it not possible to get a constant User ID from the SDK](https://devforum.zoom.us/t/is-there-a-constant-user-id-i-can-access/25359.)
        - However, a universal ID is available thru REST API if Dashboard feature is enabled. Integrating this would require access to a UBC admin account, assuming that UBC has this feature enabled
        - I have chosen to switch to a username-based identity system instead.

- Due to poor assumption of UserID, the Zoom BRB got confused who is trusted and who is not, and crashed when assigning breakout rooms (due to a combination of reasons). Mourud and I attributed this to the fact that the Zoom BRB didn't restart between meetings, and that it may work fine in the CPSC 320 lecture due to preceived success of Test 1, but in hindsight the real reason is because of a wrong assumption about Zoom's ID permanence. We decided to test it in Anne's class, and see if the Zoom BRB could assign users with tags in their name, if everyone has a tag.

# Log File 2. CPSC 320 Lecture Test (~125 participants)

## Sequence of events from log
- BRB connected to the meeting (`zoomus://ubc.zoom.us/join?confno=98589758365&uname=BRB&pwd=320320320&show_water_mark=1&meeting_views_options=0&invite_options=255&zc=0`)
- Anne makes BRB the host
- Mourud becomes trusted user, log shows Mourud's ID as 16796672, again different
- Mourud asks the TAs in Slack to private message BRB the "!trustme p@ssword" to become Co-Host
    - Log shows the TAs all eventually did so
- Mourud commands BRB to setup and assign breakout rooms.
- 50 Breakout rooms were successfully set up according to log
- Users' tags were correctly read, according to log
- 124 users were assigned to breakout rooms based on their tags.
    - Only the BRB wasn't assigned a tag
    - Everything seems smooth so far
- Mourud commands BRB to start the BO session
    - The Zoom SDK reports that starting the BO session was **unsuccessful** (no specified reason)
- Mourud sends the same command
    - This time, the Zoom SDK reports that BO session was started successfully
- **This broke Zoom**
    - Mourud and my second (non-bot) account, along with approx 30 students, were sent to a different set of breakout rooms / main meeting rooms, compared to around 100 students/instructors in the class
    - Anne was in the set with 100 students but did not have an option to leave the breakout room she was in
    - After a few minutes, the Zoom SDK crashed with "`JNI DETECTED ERROR IN APPLICATION: input is not valid Modified UTF-8: illegal start byte 0xb8`", it was my first time seeing this particular error message in all of my testing
    - Meanwhile, some students (along with my second non-bot account) were brought back to the main meeting where
    Anne and most (~100) students were
    - Mourud was still stick for a few more minutes with a handful of students in an alternative meeting, different from the one the BRB and my regular account were in (by manually accessing the BRB Android App, I could no longer see Mourud anywhere). Mourud ended that meeting, and everyone was able to reconnect to the main meeting


# Further small-scale testing:
After reimplementing the BRB to use a user's username as the source of truth rather than the Zoom UserID, I did some more thorough testing of the breakout rooms created by the Zoom SDK:

## Results: confirmed most observations from Aug 7
- StartBO call issued to Zoom SDK "failed" the first time, worked the second time
- User (regular Zoom client) received prompt to join correct breakout room
- After the Bot, using Zoom SDK created breakout rooms and/or assigned users to them, and pass Host status back to human on Zoom Client (not SDK), the human cannot see the breakout rooms
    - When Bot receives Host status back, the created breakout rooms are visible according to the SDK, and the Bot does not have to recreate them or reassign
- Bot-created Breakout Rooms function as normal
- Main meeting still worked, and user was able to re-join main meeting. The bug that occurred in the CPSC 320 class might only happen in large meetings (100+ members)

