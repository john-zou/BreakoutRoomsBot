#include "stdafx.h"
#include "sdk_demo_app.h"
#include <iostream>
#include <cstdio>

using namespace DuiLib;
int APIENTRY WinMain(HINSTANCE hInstance, HINSTANCE /*hPrevInstance*/, LPSTR /*lpCmdLine*/, int nCmdShow)
{
	freopen("johnsfile.txt", "w", stdout);
	printf("hello\n");
	std::cout << "hello std cout" << std::endl;
	HeapSetInformation(NULL, HeapEnableTerminationOnCorruption, NULL, 0);
	CSDKDemoApp app_;
	app_.Run(hInstance);
	MSG msg;
	while (GetMessage(&msg, NULL, 0, 0))
	{
		if (WM_QUIT == msg.message)
		{
			break;
		}
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}
	return 0;
}