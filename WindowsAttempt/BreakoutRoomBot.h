#pragma once
#include "stdafx.h"
#include "sdk_util.h"

class BreakoutRoomBot
{
public:
	// User status management
	void MakeHost(unsigned int zoomUserID); // "Pull the plug"
	void MakeCoHost(unsigned int zoomUserID);
	void RevokeCoHost(unsigned int zoomUserID);

	void TrustUser(unsigned int zoomUserID);
	void SetTrustPassword(std::wstring password);

	// Breakout rooms session management
	void StartBreakoutRooms();
	void EndBreakoutRooms();
	//void Announce(std::wstring message); // cannot find method

	// Breakout rooms micromanagement - before session
	void SetupBreakoutRooms();
	void MoveUserToBreakoutRoom(unsigned int zoomUserID, int roomID);

	// Breakout rooms micromanagement - during session

	// Chat Commands API (using private messages)
	void ProcessPrivateMessage(ZOOM_SDK_NAMESPACE::IUserInfo* senderUserInfo, ZOOM_SDK_NAMESPACE::IChatMsgInfo* chatMessageInfo);

	// Singleton
	static BreakoutRoomBot& GetInstance();
	BreakoutRoomBot(BreakoutRoomBot const&) = delete;
	void operator=(BreakoutRoomBot const&) = delete;

private:
	// Singleton
	BreakoutRoomBot();

	// Implementation detail
	bool isActive = true;
	bool doneSetup = false;
	std::wstring password = L"ubc";
	std::unordered_map<unsigned int, bool> trustedUsers;
	std::vector<int> breakoutRoomCounts; // Unused
};

