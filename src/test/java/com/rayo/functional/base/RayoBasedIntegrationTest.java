package com.rayo.functional.base;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.client.JmxClient;
import com.rayo.client.RayoClient;
import com.rayo.client.listener.StanzaListener;
import com.rayo.client.registry.Call;
import com.rayo.client.xmpp.stanza.Error;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.client.xmpp.stanza.Message;
import com.rayo.client.xmpp.stanza.Presence;
import com.rayo.client.xmpp.stanza.Stanza;
import com.rayo.core.verb.VerbRef;

public abstract class RayoBasedIntegrationTest {

	private Logger log = LoggerFactory.getLogger(RayoBasedIntegrationTest.class);
	
	protected RayoClient rayoClient;

	private LinkedBlockingQueue<Call> callsQueue = new LinkedBlockingQueue<Call>();
	private Map<String,List<Object>> callEvents = new ConcurrentHashMap<String, List<Object>>();
	private Map<String,List<Error>> callErrors = new ConcurrentHashMap<String, List<Error>>();
	
	private int retries = 6;
	private int waitTime = 3000;
	
	protected String xmppUsername;
	protected String xmppPassword;
	protected String sipDialUri;
	protected String xmppServer;
	protected String rayoServer;
		
	
	@Before
	public void setup() throws Exception {
		
		loadProperties();
		
		rayoClient = new RayoClient(xmppServer, rayoServer);
		rayoClient.connect(xmppUsername, xmppPassword);
		
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
						if (isRayoMesage(presence)) {
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
						if (callId != null) {
							List<Error> errors = callErrors.get(callId);
							if (errors == null) {
								errors = new ArrayList<Error>();
								callErrors.put(callId, errors);
							}
							errors.add(error);
						}
					}
				}
			}
			
			@Override
			public void onError(Error error) {

			}
		});
	}
	
	public VerbRef dial() throws Exception {
		
		return dial(new URI(sipDialUri));
	}
	
	public VerbRef dial(URI uri) throws Exception {
		
		return rayoClient.dial(uri);
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
			return callsQueue.poll(15000,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	@After
	public void shutdown() throws Exception {
	
		if (rayoClient != null) {
			rayoClient.disconnect();
		}
	}
	

	protected <T> T assertReceived(Class<T> eventClass, String callId) {
		
		int i = 0;
		log.trace(String.format("Asserting event [%s] on call [%s]. Try %s", eventClass, callId, i+1));
		List<Object> events = callEvents.get(callId);
		if (events != null) {
			do {
				T evt = null;
				for (Object event: events) {
					if (eventClass.isAssignableFrom(event.getClass())) {
						evt = (T)event;
						break;
					}
				}
				if (evt != null) {
					events.remove(evt);
					return evt;
				}
				i++;
				waitForEvents(waitTime);
			} while (i<retries);
		}
		log.error("Call event not found");
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
	
	protected void loadProperties() {

		xmppUsername = getProperty("xmpp.username", "usera");
		xmppPassword = getProperty("xmpp.password", "1");
		xmppServer = getProperty("xmpp.server", "localhost");
		rayoServer = getProperty("rayo.server", "localhost");
		sipDialUri = getProperty("sip.dial.uri", "sip:usera@127.0.0.1:5060");
	}

	private String getProperty(String property, String defaultValue) {

		String result = System.getProperty(property);
		if (result == null) {
			result = defaultValue;
		} else {
			log.trace(String.format(
					"Using system property value for [%s]:[%s]", property,
					result));
		}
		return result;
	}
	
	private boolean isRayoMesage(Stanza stanza) {
		
		return (stanza.getChildNamespace() != null && stanza.getChildNamespace().contains("urn:xmpp:rayo"));
	}
	
	protected String getNodeName() throws Exception {
		
		return getNodeName(0);
	}

	protected String getNodeName(int i) throws Exception {
		
		List<String> nodesList = new ArrayList<String>();
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
		Iterator<JSONObject> it = nodes.iterator();
		int j = 0;
		while(it.hasNext()) {
			if (j == i) {
				JSONObject json = it.next();
				String jid = (String)json.get("JID");
				return jid;
			}
			j++;
		}
		return null;
	}
}
