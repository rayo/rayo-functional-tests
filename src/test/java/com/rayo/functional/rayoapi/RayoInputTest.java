package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.rayo.client.XmppException;
import com.rayo.client.xmpp.stanza.Error.Condition;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoInputTest extends RayoBasedIntegrationTest {

	@Test
	public void testErrorOnInvalidGrammar() throws Exception {
		
		dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		try {
			rayoClient.input("<grammar>", incomingCallId);
			fail("Expected exception");
		} catch (XmppException xe) {
			assertTrue(xe.getMessage().contains("could not be compiled"));
		}
		
		assertTrue(hasAnyErrors(incomingCallId));
		assertEquals(getLastError(incomingCallId).getCondition(), Condition.bad_request);		
	}
}
