package com.rayo.functional.base;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateFormatUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.core.CallRef;
import com.voxeo.rayo.client.JmxClient;
import com.voxeo.rayo.client.RayoClient;
import com.voxeo.rayo.client.listener.StanzaListener;
import com.voxeo.rayo.client.registry.Call;
import com.voxeo.rayo.client.xmpp.stanza.Error;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Message;
import com.voxeo.rayo.client.xmpp.stanza.Presence;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;

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
	
	public CallRef dial() throws Exception {
		
		return dial(new URI(sipDialUri));
	}
	
	public CallRef dial(URI uri) throws Exception {
		
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
	

	protected <T> boolean assertNotReceived(Class<T> eventClass, String callId) {
		
		try {
			assertReceived(eventClass, callId, 0);
		} catch (AssertionError e) {
			log.error(String.format("[%s] Assertion error: [%s].",
					DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
					e.getMessage()));
			return true;
		}
		throw new AssertionError("Call Event found and was not expected");		
	}

	protected <T> T assertReceived(Class<T> eventClass, String callId) {
	
		return assertReceived(eventClass, callId, retries);
	}
	
	protected <T> T assertReceived(Class<T> eventClass, String callId, int retries) {
		
		int i = 0;
		log.trace(String.format("Asserting event [%s] on call [%s]. Try %s", eventClass, callId, i+1));
		List<Object> events = callEvents.get(callId);
			do {
				if (events != null) {
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
				}
			} while (i<retries);
		log.error("Call event not found");
		throw new AssertionError("Call Event not found");
	}

	protected void waitForEvents() {
		
		waitForEvents(1000);
	}
	
	protected void quiesceNode(JmxClient nodeClient) throws Exception {
		
		nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");
		waitUntilNodeQuiesced(nodeClient, "com.rayo:Type=Admin,name=Admin");
	}
	
	protected void dequiesceNode(JmxClient nodeClient) throws Exception {
		
		nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
		waitUntilNodeDequiesced(nodeClient, "com.rayo:Type=Admin,name=Admin");
	}
	
	protected void quiesceGateway(JmxClient nodeClient) throws Exception {
		
		nodeClient.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "enableQuiesce");
		waitUntilNodeQuiesced(nodeClient, "com.rayo.gateway:Type=Admin,name=Admin");
	}
	
	protected void dequiesceGateway(JmxClient nodeClient) throws Exception {
		
		nodeClient.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "disableQuiesce");
		waitUntilNodeDequiesced(nodeClient, "com.rayo.gateway:Type=Admin,name=Admin");
	}
	
	protected List<String> getResourcesForAppId(JmxClient nodeClient, String appId) throws Exception {
		
		JSONArray array = (JSONArray)nodeClient.jmxExec("com.rayo.gateway:Type=Gateway", "getResourcesForAppId", appId);
		
		String[] result = (String[])array.toArray(new String[]{});
		return Arrays.asList(result);
	}	
	
	private void waitUntilNodeQuiesced(JmxClient nodeClient, String url) throws Exception {

		int retries = 0;
		do {
			waitForEvents();
			boolean quiesce = (Boolean)nodeClient.jmxValue(url, "QuiesceMode");
			if (quiesce) return;
			retries++;
		} while (retries < 6);
		throw new AssertionError("Could not enter Quiesce mode on " + nodeClient);
	}
	
	private void waitUntilNodeDequiesced(JmxClient nodeClient, String url) throws Exception {

		int retries = 0;
		do {
			waitForEvents();
			boolean quiesce = (Boolean)nodeClient.jmxValue(url, "QuiesceMode");
			if (!quiesce) return;
			retries++;
		} while (retries < 6);
		throw new AssertionError("Could not enter Quiesce mode on " + nodeClient);
	}
	
	protected void waitForEvents(int time) {
		 
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {

		}
	}
	
	protected String getCallId(Stanza<?> stanza) {
		
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
		sipDialUri = getProperty("sip.dial.uri", "sip:usera@localhost:5060");
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

	protected List<String> getNodeNames() throws Exception {
		
		return getNodeNames(new JmxClient(rayoServer, "8080"));
	}
	
	protected List<String> getNodeNames(JmxClient client) throws Exception {
		
		List<String> nodesList = new ArrayList<String>();
		if ("true".equals(System.getProperty("cluster.test"))) {
			JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
			Iterator<JSONObject> it = nodes.iterator();
			int j = 0;
			while(it.hasNext()) {
				JSONObject json = it.next();
				String jid = (String)json.get("hostname");
				nodesList.add(jid);
			}
		} else {
			nodesList.add(System.getProperty("rayo.server"));
		}
		return nodesList;
	}
	
	protected List<String> getNodesForPlatform(JmxClient client, String platform) throws Exception {
		
		List<String> nodesList = new ArrayList<String>();
		JSONArray nodes = ((JSONArray)client.jmxExec("com.rayo.gateway:Type=Gateway", "getRayoNodes", platform));
		Iterator<JSONObject> it = nodes.iterator();
		int j = 0;
		while(it.hasNext()) {
			JSONObject json = it.next();
			String jid = (String)json.get("hostname");
			nodesList.add(jid);
		}
		return nodesList;
	}
	
	protected List<String> getPlatformNames(JmxClient client) throws Exception {
				
		List<String> platformsList = new ArrayList<String>();
		JSONArray platforms = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "Platforms"));
		Iterator<JSONObject> itp = platforms.iterator();
		while(itp.hasNext()) {
			JSONObject json = itp.next();
			platformsList.add((String)json.get("name"));
		}
		return platformsList;
	}
	
	protected String getNodeName(int i) throws Exception {
		
		if ("true".equals(System.getProperty("cluster.test"))) {
			JmxClient client = new JmxClient(rayoServer, "8080");
			JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
			Iterator<JSONObject> it = nodes.iterator();
			int j = 0;
			while(it.hasNext()) {
				JSONObject json = it.next();
				if (j == i) {
					String jid = (String)json.get("hostname");
					return jid;
				}
				j++;
			}
			return null;
		} else {
			return System.getProperty("rayo.server");
		}
	}
	
	protected Object getCdr(JmxClient jmx, String id) throws Exception {

		return getAttributeValue(jmx, "com.rayo:Type=Cdrs", "ActiveCDRs", "callId", id);
	}

	protected String getCdrTranscript(JmxClient jmx, String id) throws Exception {

		Object jmxObject = jmx.jmxValue("com.rayo:Type=Cdrs", "ActiveCDRs");
		JSONArray cdrs = (JSONArray) jmxObject;
		Iterator<JSONObject> it = cdrs.iterator();
		while (it.hasNext()) {
			JSONObject jsonObject = it.next();
			if (jsonObject.get("callId") != null) {
				if (jsonObject.get("callId").equals(id)) {
					return jsonObject.get("transcript").toString();
				}
			}
		}
		return null;
	}

	protected Object getAttributeValue(JmxClient jmx, String url, String node,
			String attribute, String value) throws Exception {

		Object jmxObject = jmx.jmxValue(url, node);
		if (jmxObject instanceof JSONArray) {
			JSONArray cdrs = (JSONArray) jmx.jmxValue(url, node);
			boolean found = false;
			Iterator<JSONObject> it = cdrs.iterator();
			while (it.hasNext()) {
				JSONObject object = it.next();
				if (object.get(attribute).equals(value)) {
					return object;
				}
			}
			return null;
		} else {
			JSONObject jsonObject = (JSONObject) jmxObject;
			if (jsonObject.get(attribute) != null) {
				if (jsonObject.get(attribute).equals(value)) {
					return value;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
	}

	protected String getJmxValue(JmxClient jmx, String url, String node, String attribute)
			throws Exception {

		Object jmxObject = jmx.jmxValue(url, node);
		if (jmxObject instanceof JSONArray) {
			JSONArray cdrs = (JSONArray) jmxObject;
			if (cdrs != null) {
				return cdrs.get(0).toString();
			}
			return "";
		} else {
			return jmxObject.toString();
		}
	}
	
	protected int getClientsConnected() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray clients = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "ActiveClients"));
		return clients.size();
	}

	protected long getTotalCalls() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		return ((Long)client.jmxValue("com.rayo.gateway:Type=GatewayStatistics", "TotalCallsCount"));
	}
	
	protected int getNodes() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
		return nodes.size();
	}

	protected int getActiveMixers() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray mixers = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "ActiveMixers"));
		return mixers.size();
	}
	
	protected int getActiveMixersInNodes() throws Exception {
		
		int activeMixers = 0;
		for (String nodeName : getNodeNames()) {
			JmxClient client = new JmxClient(nodeName, "8080");
			JSONArray mixers = ((JSONArray)client.jmxValue("com.rayo:Type=Mixers", "ActiveMixers"));
			activeMixers += mixers.size();
		}
		return activeMixers;
	}
	
	public List<String> getParticipants(String mixerName) throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONObject mixer = ((JSONObject)client.jmxExec("com.rayo.gateway:Type=Gateway", "mixerInfo", mixerName));
		if (mixer == null) {
			return new ArrayList<String>();
		}
		JSONArray participants = (JSONArray)mixer.get("participants");
		List<String> result = new ArrayList<String>();
		Iterator<String> it = participants.iterator();
		while(it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}
		
	public long getActiveVerbsCount(String mixerName) throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONObject mixer = ((JSONObject)client.jmxExec("com.rayo.gateway:Type=Gateway", "mixerInfo", mixerName));
		if (mixer == null) {
			return 0;
		}
		return (Long)mixer.get("activeVerbs");
	}
	
	public List<String> getActiveVerbs(String mixerName) throws Exception {
		
		List<String> verbs = new ArrayList<String>();
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray array = ((JSONArray)client.jmxExec("com.rayo.gateway:Type=Gateway", "activeVerbs", mixerName));
		Iterator<JSONObject> it = array.iterator();
		while(it.hasNext()) {
			JSONObject verb = it.next();
			verbs.add((String)verb.get("verbId"));
		}
		return verbs;
	}
	
	protected long getTotalMixers() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		return ((Long)client.jmxValue("com.rayo.gateway:Type=GatewayStatistics", "TotalMixersCount"));
	}
	
	protected long getNodeIncomingCalls(JmxClient client) throws Exception {
		
		return ((Long)client.jmxValue("com.rayo:Type=CallStatistics", "IncomingCalls"));
	}
	
	protected long getNodeActiveCalls(JmxClient client) throws Exception {
		
		return ((Long)client.jmxValue("com.rayo:Type=Calls", "ActiveCallsCount"));
	}
	
	protected long getGatewayActiveCalls(JmxClient client) throws Exception {
		
		return ((Long)client.jmxValue("com.rayo.gateway:Type=GatewayStatistics", "ActiveCallsCount"));
	}
	
	protected void disconnect(String call) {

		if (call != null) {
			try {
				rayoClient.hangup(call);
			} catch (Exception e) {
				log.error("ERROR: " + e.getMessage());
			}
		}
	}
	
	protected String getSipHostname(String sipUri) {
		
		if (sipUri.indexOf('@') != -1) {
			return sipUri.substring(sipUri.indexOf('@'));
		}
		return null;
	}
	
	protected void registerApplication(String platform, String name, String jid) throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080", "rayo/jmx");
		client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "registerApplication", platform, name, jid);
	}
	
	protected void registerAddress(String appId, String address) throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080", "rayo/jmx");
		client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "registerAddress", appId, address);
	}
	
	protected URL getResourceUrl(String filename) throws MalformedURLException {

		return new URL("http://" + rayoServer + ":8080/rayo/resources/" + filename);
	}
}
