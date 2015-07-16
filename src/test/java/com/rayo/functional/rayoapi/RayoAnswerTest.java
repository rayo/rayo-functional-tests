package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoAnswerTest extends RayoBasedIntegrationTest {

	@Test
	public void testAnswerFailsAfterHangUp() throws Exception {
		
		dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		rayoClient.hangup(incomingCallId);
		
		assertFalse(hasAnyErrors(incomingCallId));
		
		IQ result = rayoClient.answer(incomingCallId);
		assertTrue(result.isError());
		assertEquals(result.getError().getCondition(), Condition.item_not_found);
	}

	@Test
	public void testAnswerMultipleTimes() throws Exception {
		
		dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		rayoClient.answer(incomingCallId);
		rayoClient.answer(incomingCallId);
		
		waitForEvents();
		assertTrue(getLastError(incomingCallId).getText().contains("the call has already been connected or continued"));
		
		rayoClient.hangup(incomingCallId);		
		waitForEvents();
	}
	

	@Test
	public void testAcceptMultipleTimesShouldThrowErrorAndNotEndCall() throws Exception {
		
		dial().getCallId();
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
