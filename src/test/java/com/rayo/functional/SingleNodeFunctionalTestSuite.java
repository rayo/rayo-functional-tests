package com.rayo.functional;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.rayo.functional.rayoapi.CommandTest;
import com.rayo.functional.rayoapi.QuiesceTest;
import com.rayo.functional.rayoapi.RayoAnswerTest;
import com.rayo.functional.rayoapi.RayoCPATest;
import com.rayo.functional.rayoapi.RayoDtmfTest;
import com.rayo.functional.rayoapi.RayoInputTest;
import com.rayo.functional.rayoapi.RayoMiscTest;
import com.rayo.functional.rayoapi.RayoNestedJoinTest;
import com.rayo.functional.rayoapi.RayoOutputTest;
import com.rayo.functional.rayoapi.RayoRecordTest;
import com.rayo.functional.rayoapi.RayoRejectTest;
import com.rayo.functional.rayoapi.TransferTest;

@RunWith(Suite.class)
@SuiteClasses({
	AnswerTest.class,
	CdrTest.class,
	CommandTest.class,
	HangupTest.class,
	InputTest.class,
	JoinTest.class,
	MiscTest.class,
	MixerTest.class,
	MultipleJoinTest.class,
	OutputTest.class,
	QuiesceTest.class,
	RayoAnswerTest.class,
	RayoCPATest.class,
	RayoDtmfTest.class,
	RayoInputTest.class,
	RayoMiscTest.class,
	RayoNestedJoinTest.class,
	RayoOutputTest.class,
	RayoRecordTest.class,
	RayoRejectTest.class,
	RecordTest.class,
	RedirectTest.class,
	RejectTest.class,
	TransferTest.class
})
public class SingleNodeFunctionalTestSuite {

}
