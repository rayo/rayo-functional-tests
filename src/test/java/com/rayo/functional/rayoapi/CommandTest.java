package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.rayo.client.xmpp.stanza.Error.Condition;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class CommandTest extends RayoBasedIntegrationTest {

	@Test
	public void testCommandNonExistingCall() throws Exception {
		
		dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		
		IQ iq = rayoClient.hangup("aaaa");
		assertTrue(iq.isError());
		assertEquals(iq.getError().getCondition(), Condition.item_not_found);
		assertEquals(iq.getFrom(), "aaaa@localhost");
		
		rayoClient.hangup(incomingCallId);
		waitForEvents();
	}
}
