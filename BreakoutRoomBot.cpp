#include "BreakoutRoomBot.h"

inline void sendChatTo(unsigned int receiverZoomUserID, wchar_t* content) {
	SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(receiverZoomUserID, content);
}

inline bool startsWith(const wchar_t* input, const char* target) {
	return boost::istarts_with(input, target);
}

inline std::vector<std::wstring> getArgs(const wchar_t* input) {
	// https://stackoverflow.com/questions/10551125/boost-string-split-to-eliminate-spaces-in-words
	std::vector<std::wstring> args;
	boost::split(args, input, boost::is_any_of(L" "), boost::token_compress_on);
	return args;
}

inline bool areBreakoutRoomsEnabled() {
	return SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->IsBOEnabled();
}

void BreakoutRoomBot::MakeHost(unsigned int zoomUserID)
{
	SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingParticipantsController()->MakeHost(zoomUserID);
}

void BreakoutRoomBot::MakeCoHost(unsigned int zoomUserID)
{
	SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingParticipantsController()->AssignCoHost(zoomUserID);
}

void BreakoutRoomBot::RevokeCoHost(unsigned int zoomUserID) {
	SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingParticipantsController()->RevokeCoHost(zoomUserID);
}

void BreakoutRoomBot::TrustUser(unsigned int zoomUserID)
{
	this->trustedUsers[zoomUserID] = true;
}

void BreakoutRoomBot::SetTrustPassword(std::wstring password)
{
	this->password = password;
}

void BreakoutRoomBot::StartBreakoutRooms() // TODO return enum
{
	if (areBreakoutRoomsEnabled()) {
		if (!SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->IsBOStarted()) {
			SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->GetBOAdminHelper()->StartBO();
		}
	}
}

void BreakoutRoomBot::EndBreakoutRooms() // TODO return enum
{
	if (areBreakoutRoomsEnabled()) {
		if (SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->IsBOStarted()) {
			SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->GetBOAdminHelper()->StopBO();
		}
	}
}

void BreakoutRoomBot::SetupBreakoutRooms()
{
	sendChatTo(0, L"Start");
	if (areBreakoutRoomsEnabled()) {
		sendChatTo(0, L"Breakout Rooms are Enabled. Attempting to create breakout rooms...");
		
		/*if (!this->doneSetup) {
			SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->GetBOCreatorHelper()->CreateBO(L"Instructors");
			for (int i = 1; i <= 48; ++i) {
				const wchar_t* roomName = boost::lexical_cast<std::wstring>(i).c_str();
				SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->GetBOCreatorHelper()->CreateBO(roomName);
			}
			SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->GetBOCreatorHelper()->CreateBO(L"Lonewolf");
			this->doneSetup = true;
		}*/
	}
	else {
		sendChatTo(0, L"Breakout Rooms are not enabled.");
	}
}

void BreakoutRoomBot::ProcessPrivateMessage(ZOOM_SDK_NAMESPACE::IUserInfo * senderUserInfo, ZOOM_SDK_NAMESPACE::IChatMsgInfo * chatMessageInfo)
{
	auto userID = senderUserInfo->GetUserID();
	auto message = chatMessageInfo->GetContent();
	if (startsWith(message, "!hostme") || startsWith(message, "!host") || startsWith(message, "!kill")) {
		if (this->trustedUsers[userID]) {
			sendChatTo(userID, L"Making you the host.");
			this->MakeHost(userID);
			this->isActive = false;
		}
		else {
			// Default password in BreakoutRoomBot.h
			sendChatTo(userID, L"Cannot assign you Host status as you haven't entered the Breakout Room Bot password yet. Enter it with !trustme <password> e.g. !trustme 123");
		}
	}
	else if (startsWith(message, "!trustme")) {
		auto args = getArgs(message);
		if (this->trustedUsers[userID]) {
			sendChatTo(userID, L"I already trust you!");
		} else if (args.size() != 2) {
			sendChatTo(userID, L"!trustme takes 1 argument, the password.");
		}
		else if (boost::equals(args[1], this->password)) {
			this->trustedUsers[userID] = true;
			this->MakeCoHost(userID);
			sendChatTo(userID, L"OK, I trust you now!");
		}
		else {
			sendChatTo(userID, L"Invalid password.");
		}
	}
	else if (startsWith(message, "!set-pw") || startsWith(message, "!setpw") || startsWith(message, "!setpass")) {
		auto args = getArgs(message);
		if (args.size() != 2) {
			sendChatTo(userID, L"Invalid number of arguments. !setpassword takes 1 argument, the new password.");
		}
		else if (args[1].size() > 30) {
			sendChatTo(userID, L"The password is too long (max length 30 characters)");
		}
		else if (args[1].size() < 3) {
			sendChatTo(userID, L"The password is too short (min length 3 characters)");
		}
		else {
			this->SetTrustPassword(std::wstring(args[1]));
			sendChatTo(userID, L"Password changed successfully. The old password will no longer work.");
		}
	}
	else if (startsWith(message, "!startbo")) {
		if (this->trustedUsers[userID]) {
			sendChatTo(userID, L"Starting breakout rooms.");
			this->StartBreakoutRooms();
		}
		else {
			sendChatTo(userID, L"You haven't provided the Breakout Room Bot password yet. Enter it with !trustme <password> e.g. !trustme 123");
		}
	}
	else if (startsWith(message, "!endbo")) {
		if (this->trustedUsers[userID]) {
			sendChatTo(userID, L"Ending breakout rooms.");
			this->EndBreakoutRooms();
		}
	}
	else if (startsWith(message, "!room")) {
		auto args = getArgs(message);
		if (args.size() != 2) {
			sendChatTo(userID, L"!room takes 1 argument, the room name.");
		}
		// TODO

	}
	else if (startsWith(message, "!setupbo")) {
		sendChatTo(userID, L"Setting up breakout rooms");
		this->SetupBreakoutRooms();
	}
}


// Singleton
BreakoutRoomBot& BreakoutRoomBot::GetInstance()
{
	static BreakoutRoomBot instance;
	return instance;
}

BreakoutRoomBot::BreakoutRoomBot() {}