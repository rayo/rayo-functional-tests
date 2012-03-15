package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.core.JoinDestinationType;
import com.rayo.core.StartedSpeakingEvent;
import com.rayo.core.StoppedSpeakingEvent;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.rayo.client.RayoClient;
import com.voxeo.rayo.client.filter.XmppObjectExtensionNameFilter;
import com.voxeo.rayo.client.filter.XmppObjectFilter;
import com.voxeo.rayo.client.listener.RayoMessageListener;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;

public class MultipleClientActiveSpeakerTests extends RayoBasedIntegrationTest {

	private Logger log = LoggerFactory.getLogger(MultipleClientActiveSpeakerTests.class);
	
	@Before
	public void setup() throws Exception {
		
		loadProperties();
	}
	
	@After
	public void shutdown() throws Exception {}
	
	@Test
	public void testMultipleClientesGetMultipleActiveSpeakerEvents() throws Exception {
		
		String mixerId = UUID.randomUUID().toString();

		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect("usera", xmppPassword);
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect("userb", xmppPassword);			
			
			waitForEvents(2000);

			final StringBuilder callId = new StringBuilder();
			RayoMessageListener tempListener = new RayoMessageListener("offer") {
				
				@Override
				@SuppressWarnings("rawtypes")
				public void messageReceived(Object object) {
					
					Stanza stanza = (Stanza)object;
					callId.append(stanza.getFrom().substring(0, stanza.getFrom().indexOf('@')));
				}
			};
			rayoClient1.addStanzaListener(tempListener);
			rayoClient2.addStanzaListener(tempListener);
			
			XmppObjectFilter started1 = new XmppObjectExtensionNameFilter("started-speaking");
			XmppObjectFilter stopped1 = new XmppObjectExtensionNameFilter("stopped-speaking");
			XmppObjectFilter started2 = new XmppObjectExtensionNameFilter("started-speaking");
			XmppObjectFilter stopped2 = new XmppObjectExtensionNameFilter("stopped-speaking");
			
			rayoClient1.addFilter(started1);
			rayoClient1.addFilter(stopped1);
			rayoClient2.addFilter(started2);
			rayoClient2.addFilter(stopped2);
			
			// Now we dial 4 times from the same client and each of the filter 
			// should be hit twice which means each client got two offers
			String outgoing1 = rayoClient1.dial(new URI("sip:usera@localhost")).getCallId();
			waitForEvents(100);
			String incoming1 = callId.toString();
			callId.delete(0, callId.length());
			
			String outgoing2 = rayoClient2.dial(new URI("sip:userb@localhost")).getCallId();
			waitForEvents(100);
			String incoming2 = callId.toString();
			
			rayoClient1.answer(incoming1);
			rayoClient2.answer(incoming2);

			IQ iq = rayoClient1.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
			assertTrue(iq.isResult());

			iq = rayoClient2.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
			assertTrue(iq.isResult());

			rayoClient1.output("Hello this is a short phrase.", outgoing1);			
			waitForEvents(2000);
			
			assertNotNull(started1.poll());
			assertNotNull(stopped1.poll());
			assertNotNull(started2.poll());
			assertNotNull(stopped2.poll());
			
			iq = rayoClient1.unjoin(mixerId, JoinDestinationType.MIXER, outgoing1);
			assertTrue(iq.isResult());

			iq = rayoClient2.unjoin(mixerId, JoinDestinationType.MIXER, outgoing2);
			assertTrue(iq.isResult());
			
			waitForEvents();

			rayoClient1.hangup(outgoing1);
			rayoClient2.hangup(outgoing2);
			waitForEvents();		

		} finally {
			try {
				if (rayoClient1 != null && rayoClient1.getXmppConnection().isConnected()) {
					rayoClient1.disconnect();
				}
			} finally {
				if (rayoClient2 != null && rayoClient2.getXmppConnection().isConnected()) {
					rayoClient2.disconnect();
				}
			}
		}		
	}

}
