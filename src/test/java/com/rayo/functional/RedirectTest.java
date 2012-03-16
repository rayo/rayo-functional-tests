package com.rayo.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.CallCompleteEvent.Cause;

public class RedirectTest extends MohoBasedIntegrationTest {

	@Test
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
		    CallCompleteEvent complete1 = assertReceived(CallCompleteEvent.class, incoming1);
		    assertEquals(complete1.getCause(), Cause.REDIRECT);
		    CallCompleteEvent complete2 = assertReceived(CallCompleteEvent.class, outgoing1);
		    assertEquals(complete2.getCause(), Cause.REDIRECT);
		    
		} finally {}
	}
	
	@Test
	public void testRedirectAccepted() {

		OutgoingCall outgoing1 = null;
		try {
		    outgoing1 = dial();
		    
		    IncomingCall incoming1 = getIncomingCall();
		    assertNotNull(incoming1);
		    incoming1.accept();

			CallableEndpoint endpoint = (CallableEndpoint) mohoRemote
					.createEndpoint(URI.create(getSipDialUri()));
			
		    incoming1.redirect(endpoint);
		    waitForEvents(500);
		    CallCompleteEvent complete1 = assertReceived(CallCompleteEvent.class, incoming1);
		    assertEquals(complete1.getCause(), Cause.REDIRECT);
		    CallCompleteEvent complete2 = assertReceived(CallCompleteEvent.class, outgoing1);
		    assertEquals(complete2.getCause(), Cause.REDIRECT);
		    
		} finally {}
	}

	@Test
	public void testFailToRedirectAnsweredCall() {

		OutgoingCall outgoing1 = null;
		try {
		    outgoing1 = dial();
		    
		    IncomingCall incoming1 = getIncomingCall();
		    assertNotNull(incoming1);
		    incoming1.answer();

			CallableEndpoint endpoint = (CallableEndpoint) mohoRemote
					.createEndpoint(URI.create(getSipDialUri()));
			
			try {
				incoming1.redirect(endpoint);
				fail("Expected error");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("You can't redirect a call that has already been answered"));
			}
		} finally {
			try {
				outgoing1.hangup();
			} catch (Exception e) {}
		}
	}	
}
