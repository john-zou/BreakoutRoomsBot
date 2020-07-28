#pragma once
#include "StdAfx.h"
#include "sdk_util.h"

class UbcApi
{
public:
    void foo();

	void makeHost(unsigned int userID);

    // Singleton
    static UbcApi& getInstance();
    UbcApi(UbcApi const&) = delete;
    void operator=(UbcApi const&) = delete;
private:
    UbcApi();

    // TODO: Add modes here
};
