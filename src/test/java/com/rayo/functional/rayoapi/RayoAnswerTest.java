package com.rayo.functional.rayoapi;

import java.net.URI;

import org.junit.Test;
import static org.junit.Assert.*;

import com.rayo.client.xmpp.stanza.Error.Condition;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoAnswerTest extends RayoBasedIntegrationTest {

	@Test
	public void testAnswerFailsAfterHangUp() throws Exception {
		
		rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		rayoClient.hangup(incomingCallId);
		
		assertFalse(hasAnyErrors(incomingCallId));
		
		rayoClient.answer(incomingCallId);
		assertTrue(hasAnyErrors(incomingCallId));
		assertEquals(getLastError(incomingCallId).getCondition(), Condition.item_not_found);
	}

	@Test
	public void testAnswerMultipleTimes() throws Exception {
		
		rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		rayoClient.answer(incomingCallId);
		rayoClient.answer(incomingCallId);
		
		waitForEvents();
		assertFalse(hasAnyErrors(incomingCallId));
		rayoClient.hangup(incomingCallId);		
		waitForEvents();
	}
	

	@Test
	public void testAcceptMultipleTimesShouldThrowErrorAndNotEndCall() throws Exception {
		
		rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.accept(incomingCallId);
		rayoClient.accept(incomingCallId);
		
		waitForEvents();
		assertTrue(hasAnyErrors(incomingCallId));
		assertEquals(getLastError(incomingCallId).getText(), "Call is already accepted");
		rayoClient.hangup(incomingCallId);		
		waitForEvents();
	}
}
