package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoOutputTest extends RayoBasedIntegrationTest {

	@Test
	public void testCantOutputNonAnsweredCall() throws Exception {
		
		String outgoing = dial().getCallId();
		try {
			String incomingCallId = getIncomingCall().getCallId();
			
			try {
				rayoClient.output("hello",incomingCallId);
				fail("Expected exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("The call has not been answered"));
			}
		} finally {
			rayoClient.hangup(outgoing);
		}
	}
}
