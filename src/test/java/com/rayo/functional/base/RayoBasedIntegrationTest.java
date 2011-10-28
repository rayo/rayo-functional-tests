package com.rayo.functional.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import com.rayo.client.RayoClient;
import com.rayo.client.listener.StanzaListener;
import com.rayo.client.registry.Call;
import com.rayo.client.xmpp.stanza.Error;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.client.xmpp.stanza.Message;
import com.rayo.client.xmpp.stanza.Presence;
import com.rayo.client.xmpp.stanza.Stanza;

public abstract class RayoBasedIntegrationTest {

	protected RayoClient rayoClient;

	private LinkedBlockingQueue<Call> callsQueue = new LinkedBlockingQueue<Call>();
	private Map<String,List<Object>> callEvents = new ConcurrentHashMap<String, List<Object>>();
	private Map<String,List<Error>> callErrors = new ConcurrentHashMap<String, List<Error>>();
	
	private int retries = 6;
	private int waitTime = 3000;
	
	@Before
	public void setup() throws Exception {
		
		rayoClient = new RayoClient("localhost", "localhost");
		rayoClient.connect("usera", "1");
		
		rayoClient.addStanzaListener(new StanzaListener() {
			
			@Override
			public void onPresence(Presence presence) {
				
				String callId = getCallId(presence);
				if (presence.getChildName() != null && presence.getChildName().equals("offer")) {
					String domain = getDomain(presence);
					Call call = new Call(callId, domain);
					callsQueue.add(call);
				}
					
				if (presence.hasExtension() && presence.getShow() == null) {
					synchronized(this) {
						Object object = presence.getExtension().getObject();
						List<Object> events = callEvents.get(callId);
						if (events == null) {
							events = new ArrayList<Object>();
							callEvents.put(callId, events);
						}
						events.add(object);
					}
				}
			}
			
			@Override
			public void onMessage(Message message) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onIQ(IQ iq) {
				
				if (iq.isError()) {
					String callId = getCallId(iq);
					synchronized(this) {
						Error error = iq.getError();
						List<Error> errors = callErrors.get(callId);
						if (errors == null) {
							errors = new ArrayList<Error>();
							callErrors.put(callId, errors);
						}
						errors.add(error);
					}
				}
			}
			
			@Override
			public void onError(Error error) {

			}
		});
	}
	
	public boolean hasAnyErrors(String callId) {
		
		List<Error> errors = callErrors.get(callId);
		if (errors == null || errors.isEmpty()) {
			return false;
		}
		return true;
	}
	
	public Error getLastError(String callId) {
		
		List<Error> errors = callErrors.get(callId);
		if (errors == null || errors.isEmpty()) {
			return null;
		}
		return errors.get(errors.size()-1);

	}
	
	protected Call getIncomingCall() {

		try {
			return callsQueue.poll(5000,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	@After
	public void shutdown() throws Exception {
	
		rayoClient.disconnect();
	}
	

	protected <T> T assertReceived(Class<T> eventClass, Call call) {
		
		int i = 0;
		System.out.println(String.format("Asserting event [%s] on call [%s]. Try %s", eventClass, call.getCallId(), i+1));
		List<Object> events = callEvents.get(call.getCallId());
		if (events != null) {
			do {
				for (Object event: events) {
					if (eventClass.isAssignableFrom(event.getClass())) {
						return (T)event;
					}
				}
				i++;
				waitForEvents(waitTime);
			} while (i<retries);
		}
		System.out.println("Call event not found");
		throw new AssertionError("Call Event not found");
	}

	protected void waitForEvents() {
		
		waitForEvents(1000);
	}
	
	protected void waitForEvents(int time) {
		 
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {

		}
	}
	
	private String getCallId(Stanza<?> stanza) {
		
		String callId = null;
		int at = stanza.getFrom().indexOf('@');
		if (at != -1) {
			callId = stanza.getFrom().substring(0, at);
		}
		return callId;
	}
	

	private String getDomain(Stanza<?> stanza) {
		
		String domain = null;
		int at = stanza.getFrom().indexOf('@');
		if (at != -1) {
			domain = stanza.getFrom().substring(at+1);
			if (domain.contains(":")) {
				domain = domain.substring(0, domain.indexOf(':'));
			}
		}
		return domain;
	}
}
