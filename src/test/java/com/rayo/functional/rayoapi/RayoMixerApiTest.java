package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.core.JoinDestinationType;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;

public class RayoMixerApiTest extends RayoBasedIntegrationTest {

	@Test
	public void testJoinAndUnjoin() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		IQ iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	

	@Test
	public void testJoinAndUnjoinMultipleCalls() throws Exception {
		
		String outgoing1 = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incoming1 = getIncomingCall().getCallId();		
		rayoClient.answer(incoming1);
		waitForEvents();

		String outgoing2 = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incoming2 = getIncomingCall().getCallId();		
		rayoClient.answer(incoming2);
		waitForEvents();

		String outgoing3 = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incoming3 = getIncomingCall().getCallId();		
		rayoClient.answer(incoming3);
		waitForEvents();

		IQ iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming3);
		assertTrue(iq.isResult());
		
		waitForEvents(3000);
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming3);
		assertTrue(iq.isResult());
		
		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		rayoClient.hangup(outgoing3);
		waitForEvents(3000);
	}
	
	@Test
	@Ignore
	public void testOutputOnMixer() throws Exception {
		
		String outgoing1 = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);

		IQ iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());

		waitForEvents();
		rayoClient.input("yes,no", incoming1);
		rayoClient.input("yes,no", incoming2);

		waitForEvents();
		rayoClient.output("yes", "1234");
		
		// Expect input completes. Does not work
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, outgoing1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, outgoing2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
		
	}
	
}
