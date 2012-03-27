package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoinedEvent;
import com.rayo.core.UnjoinedEvent;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.rayo.client.RayoClient;
import com.voxeo.rayo.client.filter.XmppObjectExtensionNameFilter;
import com.voxeo.rayo.client.filter.XmppObjectFilter;
import com.voxeo.rayo.client.listener.RayoMessageListener;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;

public class MultipleClientMixerTest extends RayoBasedIntegrationTest {
	
	URI uri1;
	URI uri2;
	URI uri3;
	URI uri4;
	final StringBuilder callId = new StringBuilder();
	RayoMessageListener tempListener;
	RayoMessageListener joinedListener;
	RayoMessageListener unjoinedListener;
	String xmppUser1="test1";
	String xmppPassword1="p@ssword1";
	String xmppUser2="test2";
	String xmppPassword2="p@ssword2";	
	String xmppUser3="test3";
	String xmppPassword3="p@ssword3";
	String xmppUser4="test4";
	String xmppPassword4="p@ssword4";
	
	final LinkedBlockingQueue<Stanza> joinQueue = new LinkedBlockingQueue<Stanza>();
	final LinkedBlockingQueue<Stanza> unjoinQueue = new LinkedBlockingQueue<Stanza>();
	
	@Before
	public void setup() throws Exception {
		
		loadProperties();
		
		joinQueue.clear();
		unjoinQueue.clear();
		String hostname = getSipHostname(sipDialUri);
		uri1 = new URI("sip:test1" + hostname);
		uri2 = new URI("sip:test2" + hostname);
		uri3 = new URI("sip:test3" + hostname);
		uri4 = new URI("sip:test4" + hostname);
		
		registerApplication("staging", "appb", xmppUser1 + "@" + xmppServer);
		registerAddress(xmppUser1 + "@" + xmppServer, uri1.toString());
		registerApplication("staging", "appb", xmppUser2 + "@" + xmppServer);
		registerAddress(xmppUser2 + "@" + xmppServer, uri2.toString());
		registerApplication("staging", "appc", xmppUser3 + "@" + xmppServer);
		registerAddress(xmppUser3 + "@" + xmppServer, uri3.toString());
		registerApplication("staging", "appd", xmppUser4 + "@" + xmppServer);
		registerAddress(xmppUser4 + "@" + xmppServer, uri4.toString());
		
		tempListener = new RayoMessageListener("offer") {
			
			@Override
			@SuppressWarnings("rawtypes")
			public void messageReceived(Object object) {
				
				Stanza stanza = (Stanza)object;
				callId.append(stanza.getFrom().substring(0, stanza.getFrom().indexOf('@')));
			}
		};
		
		joinedListener = new RayoMessageListener("joined") {
			
			@Override
			@SuppressWarnings("rawtypes")
			public void messageReceived(Object object) {
				
				Stanza stanza = (Stanza)object;
				joinQueue.add(stanza);
			}
		};
		
		unjoinedListener = new RayoMessageListener("unjoined") {
			
			@Override
			@SuppressWarnings("rawtypes")
			public void messageReceived(Object object) {
				
				Stanza stanza = (Stanza)object;
				unjoinQueue.add(stanza);
			}
		};
	}
	
	@After
	public void shutdown() throws Exception {}
	
	
	@Test
	public void testJoinedUnjoinedEventsFromMixer() throws Exception {
		
		String mixerId = UUID.randomUUID().toString();
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		RayoClient rayoClient3 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUser2, xmppPassword2);
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUser3, xmppPassword3);			
			rayoClient3 = new RayoClient(xmppServer, rayoServer);
			rayoClient3.connect(xmppUser4, xmppPassword4);	
			
			waitForEvents(2000);

			rayoClient1.addStanzaListener(tempListener);
			rayoClient2.addStanzaListener(tempListener);
			rayoClient3.addStanzaListener(tempListener);
			
			rayoClient1.addStanzaListener(joinedListener);
			rayoClient2.addStanzaListener(joinedListener);
			rayoClient3.addStanzaListener(joinedListener);

			rayoClient1.addStanzaListener(unjoinedListener);
			rayoClient2.addStanzaListener(unjoinedListener);
			rayoClient3.addStanzaListener(unjoinedListener);
			
			String outgoing1 = rayoClient1.dial(uri2).getCallId();
			waitForEvents(2000);
			String incoming1 = new String(callId);
			callId.delete(0, callId.length());
			
			String outgoing2 = rayoClient2.dial(uri3).getCallId();
			waitForEvents(2000);
			String incoming2 = new String(callId);
			callId.delete(0, callId.length());
			
			String outgoing3 = rayoClient3.dial(uri4).getCallId();
			waitForEvents(2000);
			String incoming3 = new String(callId);
			callId.delete(0, callId.length());
			
			rayoClient1.answer(incoming1);
			rayoClient2.answer(incoming2);
			rayoClient3.answer(incoming3);

			// 1st join. 1 call event, 1 participant event
			IQ iq = rayoClient1.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateJoins(joinQueue, xmppUser2, incoming1, mixerId, false, xmppUser2);

			// 2nd join. 1 call event, 2 participant events
			iq = rayoClient2.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateJoins(joinQueue, xmppUser3,incoming2, mixerId, false, xmppUser2, xmppUser3);

			// 3rd join. 1 call event, 3 participant events
			iq = rayoClient3.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming3);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateJoins(joinQueue, xmppUser4,incoming3, mixerId, false, xmppUser2, xmppUser3, xmppUser4);
			
			// 1st unjoin. 1 call event, 3 participant events
			iq = rayoClient1.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateUnjoins(unjoinQueue, xmppUser2,incoming1, mixerId, false, xmppUser2, xmppUser3, xmppUser4);

			// 2nd unjoin. 1 call event, 2 participant events
			iq = rayoClient2.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateUnjoins(unjoinQueue, xmppUser3,incoming2, mixerId, false, xmppUser3, xmppUser4);

			// 3rd unjoin. 1 call event, 1 participant events
			iq = rayoClient3.unjoin(mixerId, JoinDestinationType.MIXER, incoming3);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateUnjoins(unjoinQueue, xmppUser4,incoming3, mixerId, false, xmppUser4);

			rayoClient1.hangup(outgoing1);
			rayoClient2.hangup(outgoing2);
			rayoClient3.hangup(outgoing3);
			waitForEvents();		

		} finally {
			try {
				if (rayoClient1 != null && rayoClient1.getXmppConnection().isConnected()) {
					rayoClient1.disconnect();
				}
			} finally {
				try {
					if (rayoClient2 != null && rayoClient2.getXmppConnection().isConnected()) {
						rayoClient2.disconnect();	
					}
				} finally {
					if (rayoClient3 != null && rayoClient3.getXmppConnection().isConnected()) {
						rayoClient3.disconnect();
					}
				}
			}
		}		
	}
	
	private void validateJoins(LinkedBlockingQueue<Stanza> joinQueue,
			String sender, String callId, String mixerId, boolean validateParticipantEvents, String... participants) {

		List<Stanza> stanzas = new ArrayList<Stanza>();
		while(!joinQueue.isEmpty()) {
			stanzas.add(joinQueue.poll());
		}
		assertEquals(stanzas.size(), participants.length + 1);
		
		boolean foundCallEvent = false;
		for (Stanza stanza: stanzas) {
			String from = stanza.getFrom().substring(0, stanza.getFrom().indexOf('@'));
			String to = stanza.getTo().substring(0, stanza.getTo().indexOf('@'));
			if (sender.equals(to)) {
				JoinedEvent joined = (JoinedEvent)stanza.getExtension().getObject();
				if (joined.getTo().equals(mixerId)) {
					if (from.equals(callId)) {
						foundCallEvent = true;
						break;
					}
				}
			}
		}
		if (!foundCallEvent) {
			fail("Joined event with mixer-name attribute not received");
		}
		
		// Validate participant events
		if (validateParticipantEvents) {
			for(String participant: participants) {
				boolean participantEventFound = false;
				for (Stanza stanza: stanzas) {
					String from = stanza.getFrom().substring(0, stanza.getFrom().indexOf('@'));
					String to = stanza.getTo().substring(0, stanza.getTo().indexOf('@'));
					if (to.equals(participant) && from.equals(mixerId)) {
						participantEventFound = true;
						break;
					}
				}	
				if (!participantEventFound) {
					fail("Participant event for call id " + participant + " was not received");
				}
			}
		}
	}
	
	
	private void validateUnjoins(LinkedBlockingQueue<Stanza> unjoinQueue,
			String sender, String callId, String mixerId, boolean validateParticipantEvents, String... participants) {

		List<Stanza> stanzas = new ArrayList<Stanza>();
		while(!unjoinQueue.isEmpty()) {
			stanzas.add(unjoinQueue.poll());
		}
		assertEquals(stanzas.size(), participants.length + 1);
		
		boolean foundCallEvent = false;
		for (Stanza stanza: stanzas) {
			String from = stanza.getFrom().substring(0, stanza.getFrom().indexOf('@'));
			String to = stanza.getTo().substring(0, stanza.getTo().indexOf('@'));
			if (sender.equals(to)) {
				UnjoinedEvent unjoined = (UnjoinedEvent)stanza.getExtension().getObject();
				if (unjoined.getFrom().equals(mixerId)) {
					if (from.equals(callId)) {
						foundCallEvent = true;
						break;
					}
				}
			}
		}
		if (!foundCallEvent) {
			fail("Joined event with mixer-name attribute not received");
		}
		
		// Validate participant events
		for(String participant: participants) {
			boolean participantEventFound = false;
			for (Stanza stanza: stanzas) {
				String from = stanza.getFrom().substring(0, stanza.getFrom().indexOf('@'));
				String to = stanza.getTo().substring(0, stanza.getTo().indexOf('@'));
				if (to.equals(participant) && from.equals(mixerId)) {
					participantEventFound = true;
					break;
				}
			}	
			if (!participantEventFound) {
				fail("Participant event for call id " + participant + " was not received");
			}
		}
	}

	@Test
	public void testMultipleClientesGetMultipleActiveSpeakerEvents() throws Exception {
		
		String mixerId = UUID.randomUUID().toString();

		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUser1, xmppPassword1);
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUser2, xmppPassword2);			
			
			waitForEvents(2000);

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
			
			String outgoing1 = rayoClient1.dial(uri1).getCallId();
			waitForEvents(2000);
			String incoming1 = callId.toString();
			callId.delete(0, callId.length());
			
			String outgoing2 = rayoClient2.dial(uri2).getCallId();
			waitForEvents(2000);
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
	
	
	@Test
	public void testParticipantEvents() throws Exception {
		
		String mixerId = UUID.randomUUID().toString();
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		RayoClient rayoClient3 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUser2, xmppPassword2);
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUser3, xmppPassword3);			
			rayoClient3 = new RayoClient(xmppServer, rayoServer);
			rayoClient3.connect(xmppUser4, xmppPassword4);	
			
			waitForEvents(2000);

			rayoClient1.addStanzaListener(tempListener);
			rayoClient2.addStanzaListener(tempListener);
			rayoClient3.addStanzaListener(tempListener);
			
			rayoClient1.addStanzaListener(joinedListener);
			rayoClient2.addStanzaListener(joinedListener);
			rayoClient3.addStanzaListener(joinedListener);

			rayoClient1.addStanzaListener(unjoinedListener);
			rayoClient2.addStanzaListener(unjoinedListener);
			rayoClient3.addStanzaListener(unjoinedListener);
			
			String outgoing1 = rayoClient1.dial(uri2).getCallId();
			waitForEvents(2000);
			String incoming1 = new String(callId);
			callId.delete(0, callId.length());
			
			String outgoing2 = rayoClient2.dial(uri3).getCallId();
			waitForEvents(2000);
			String incoming2 = new String(callId);
			callId.delete(0, callId.length());
			
			String outgoing3 = rayoClient2.dial(uri4).getCallId();
			waitForEvents(2000);
			String incoming3 = new String(callId);
			callId.delete(0, callId.length());
			
			rayoClient1.answer(incoming1);
			rayoClient2.answer(incoming2);
			rayoClient3.answer(incoming3);

			// 1st join. 1 call event, 1 participant event
			IQ iq = rayoClient1.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateJoins(joinQueue, xmppUser2, incoming1, mixerId, true, xmppUser2);

			// 2nd join. 1 call event, 2 participant events
			iq = rayoClient2.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateJoins(joinQueue, xmppUser3,incoming2, mixerId, true, xmppUser2, xmppUser3);

			// 3rd join. 1 call event, 3 participant events
			iq = rayoClient3.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming3);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateJoins(joinQueue, xmppUser4,incoming3, mixerId, true, xmppUser2, xmppUser3, xmppUser4);
			
			// 1st unjoin. 1 call event, 3 participant events
			iq = rayoClient1.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateUnjoins(unjoinQueue, xmppUser2,incoming1, mixerId, true, xmppUser2, xmppUser3, xmppUser4);

			// 2nd unjoin. 1 call event, 2 participant events
			iq = rayoClient2.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateUnjoins(unjoinQueue, xmppUser3,incoming2, mixerId, true, xmppUser3, xmppUser4);

			// 3rd unjoin. 1 call event, 1 participant events
			iq = rayoClient3.unjoin(mixerId, JoinDestinationType.MIXER, incoming3);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateUnjoins(unjoinQueue, xmppUser4,incoming3, mixerId, true, xmppUser4);

			rayoClient1.hangup(outgoing1);
			rayoClient2.hangup(outgoing2);
			rayoClient2.hangup(outgoing3);
			waitForEvents();		

		} finally {
			try {
				if (rayoClient1 != null && rayoClient1.getXmppConnection().isConnected()) {
					rayoClient1.disconnect();
				}
			} finally {
				try {
					if (rayoClient2 != null && rayoClient2.getXmppConnection().isConnected()) {
						rayoClient2.disconnect();	
					}
				} finally {
					if (rayoClient3 != null && rayoClient3.getXmppConnection().isConnected()) {
						rayoClient3.disconnect();
					}
				}
			}
		}		
	}

	@Test
	public void testUnsubscribeMixerEvents() throws Exception {
		
		String mixerId = UUID.randomUUID().toString();
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUser2, xmppPassword2);
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUser3, xmppPassword3);			
			
			waitForEvents(2000);

			rayoClient1.addStanzaListener(tempListener);
			rayoClient2.addStanzaListener(tempListener);
			
			rayoClient1.addStanzaListener(joinedListener);
			rayoClient2.addStanzaListener(joinedListener);

			rayoClient1.addStanzaListener(unjoinedListener);
			rayoClient2.addStanzaListener(unjoinedListener);
			
			String outgoing1 = rayoClient1.dial(uri2).getCallId();
			waitForEvents(2000);
			String incoming1 = new String(callId);
			callId.delete(0, callId.length());
			
			String outgoing2 = rayoClient2.dial(uri3).getCallId();
			waitForEvents(2000);
			String incoming2 = new String(callId);
			callId.delete(0, callId.length());
			
			rayoClient1.answer(incoming1);
			rayoClient2.answer(incoming2);
			
			// 1st join. 1 call event, 1 participant event
			IQ iq = rayoClient1.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateJoins(joinQueue, xmppUser2, incoming1, mixerId, true, xmppUser2);

			// unsubscribe
			rayoClient1.unavailable(mixerId);
			waitForEvents(500);

			// 2nd join. In theory 1 call event, 2 participant events
			iq = rayoClient2.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
			waitForEvents(500);
			assertTrue(iq.isResult());
			// But as test2 unsubscribed only test3 will receive the joined event from mixer
			validateJoins(joinQueue, xmppUser3,incoming2, mixerId, true, xmppUser3);
			
			// Again 2 mixer events in theory
			iq = rayoClient2.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
			waitForEvents(500);
			assertTrue(iq.isResult());
			// But as test2 unsubscribed only test3 will receive the joined event from mixer
			validateUnjoins(unjoinQueue, xmppUser3,incoming2, mixerId, true, xmppUser3);

			// resubscribe
			rayoClient1.available(mixerId); 
			waitForEvents(500);
			
			// 2nd unjoin. Available. It will get the unjoin
			iq = rayoClient1.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
			waitForEvents(500);
			assertTrue(iq.isResult());
			validateUnjoins(unjoinQueue, xmppUser2,incoming1, mixerId, true, xmppUser2);

			// end
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
