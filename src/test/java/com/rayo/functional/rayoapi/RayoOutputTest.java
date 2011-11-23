package com.rayo.functional.rayoapi;

import org.junit.Test;

import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoOutputTest extends RayoBasedIntegrationTest {

	@Test
	public void testCantOutputNonAnsweredCall() throws Exception {
		
		dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.output("hello",incomingCallId);
		waitForEvents();

	}
}
