#include "UbcApi.h"

UbcApi::UbcApi() {}


void UbcApi::foo()
{
	SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(0, L"Get Participants List Count: ");
	auto list = SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingParticipantsController()->GetParticipantsList();
	auto count = list->GetCount();
	SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(0, const_cast<wchar_t*>(std::to_wstring(count).c_str()));
	for (int i = 1; i < count; ++i) { // i = 0 is 0, for everyone
		auto elem = list->GetItem(i);
		auto user = SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingParticipantsController()->GetUserByUserID(elem);
		auto userName = user->GetUserNameW();
		auto role = user->GetUserRole();
		SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(0, const_cast<wchar_t*>(std::to_wstring(elem).c_str()));
		SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(0, const_cast<wchar_t*>(userName));
		SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(0, const_cast<wchar_t*>(std::to_wstring(role).c_str()));
	}
}

void UbcApi::makeHost(unsigned int userID) {
	if (SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingBOController()->IsBOEnabled()) {
		SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(0, L"BO Rooms are enabled.");
	}
	else {
		SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(0, L"BO Rooms are not enabled.");
	}

	SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingChatController()->SendChatTo(0, L"Making you the host...");
	auto err = SDKInterfaceWrap::GetInst().GetMeetingService()->GetMeetingParticipantsController()->MakeHost(userID);
	}


UbcApi& UbcApi::getInstance()
{
    static UbcApi instance;
    return instance;
}
