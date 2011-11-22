package com.rayo.functional;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.event.AnsweredEvent;
import com.voxeo.moho.event.CallCompleteEvent.Cause;
import com.voxeo.moho.sip.SIPCall.State;

public class RedirectTest extends MohoBasedIntegrationTest {

	@Test
	@Ignore
	//TODO:  #1598098
	public void testRedirect() {

		OutgoingCall outgoing1 = null;
		try {
		    outgoing1 = dial();
		    
		    IncomingCall incoming1 = getIncomingCall();
		    assertNotNull(incoming1);

			CallableEndpoint endpoint = (CallableEndpoint) mohoRemote
					.createEndpoint(URI.create(getSipDialUri()));
			
		    incoming1.redirect(endpoint);
		    waitForEvents(500);
		    assertEquals(incoming1.getCallState(), State.REDIRECTED);
		} finally {
			outgoing1.hangup();
		}
	}
}
