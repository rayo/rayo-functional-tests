package com.rayo.functional.base;

import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateFormatUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;

import com.rayo.client.JmxClient;
import com.rayo.core.verb.Ssml;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.common.event.MohoMediaCompleteEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.media.MediaOperation;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;
import com.voxeo.moho.remote.sample.SimpleAuthenticateCallbackImpl;

public abstract class MohoBasedIntegrationTest {

	private LinkedBlockingQueue<IncomingCall> callsQueue = new LinkedBlockingQueue<IncomingCall>();
	private List<Event> events = new ArrayList<Event>();

	private List<OutgoingCall> outgoingCalls = new ArrayList<OutgoingCall>();
	private List<IncomingCall> incomingCalls = new ArrayList<IncomingCall>();

	private MohoRemote mohoRemote;

	private int retries = 6;
	private int waitTime = 3000;
	private int next = 0;

	private String xmppUsername;
	private String xmppPassword;
	private List<String> sipDialUris = new ArrayList<String>();
	private String xmppServer;
	private String rayoServer;

	@Before
	public void setup() {

		callsQueue.clear();
		events.clear();
		incomingCalls.clear();
		outgoingCalls.clear();

		mohoRemote = new MohoRemoteImpl();
		mohoRemote.addObserver(new MohoObserver(this));

		loadProperties();

		mohoRemote.connect(new SimpleAuthenticateCallbackImpl(xmppUsername,
				xmppPassword, "", "voxeo3"), xmppServer, rayoServer);
		
		try {
			// Wait 1 seconds to get presence events propagated		
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
	}

	private void loadProperties() {

		xmppUsername = getProperty("xmpp.username", "usera");
		xmppPassword = getProperty("xmpp.password", "1");
		xmppServer = getProperty("xmpp.server", "localhost");
		rayoServer = getProperty("rayo.server", "localhost");
		String[] uris = getProperty("sip.dial.uri", "sip:usera@127.0.0.1:5060").split(",");
		sipDialUris.addAll(Arrays.asList(uris));
	}

	private String getProperty(String property, String defaultValue) {

		String result = System.getProperty(property);
		if (result == null) {
			result = defaultValue;
		} else {
			System.out.println(String.format(
					"Using system property value for [%s]:[%s]", property,
					result));
		}
		return result;
	}

	@After
	public void shutdown() {

		try {
			for (OutgoingCall call : outgoingCalls) {
				try {
					call.disconnect();
				} catch (Exception e) {
					System.out.println(String.format(
							"[ERROR] Problem disconnecting outgoing call [%s]",
							call.getId()));
				}
			}

			mohoRemote.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public OutgoingCall dial() {
		
		return dial(getSipDialUri());
	}
	
	public OutgoingCall dial(String uri) {

		CallableEndpoint endpoint = (CallableEndpoint) mohoRemote
				.createEndpoint(URI.create(uri));
		Call call = endpoint.createCall("sip:test@test.com");
		call.addObserver(new MohoObserver(this));
		call.join();

		outgoingCalls.add((OutgoingCall) call);

		return (OutgoingCall) call;
	}

	protected synchronized IncomingCall getIncomingCall() {

		try {
			IncomingCall call = callsQueue.poll(15000, TimeUnit.MILLISECONDS);
			if (call != null) {
				incomingCalls.add(call);
			}
			return call;
		} catch (InterruptedException e) {
			return null;
		}
	}

	protected <T> T assertReceived(Class<T> eventClass, MediaOperation operation) {

		return assertReceived(eventClass, operation, retries);
	}

	protected <T> T assertReceived(Class<T> eventClass,
			MediaOperation operation, int retries) {

		int i = 0;
		do {
			synchronized (events) {
				T evt = null;
				System.out.println(String.format(
						"[%s] Asserting event [%s] on operation [%s]. Try %s",
						DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
						eventClass, operation, i + 1));
				for (Event event : events) {
					System.out.println(String.format(
							"[%s] Checking if event [%s] is assignable.",
							DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
							event.getClass()));
					if (eventClass.isAssignableFrom(event.getClass())) {
						System.out
								.println(String
										.format("[%s] Checking if event [%s] is media complete.",
												DateFormatUtils.format(
														new Date(),
														"hh:mm:ss.SSS"), event
														.getClass()));
						if (event instanceof MohoMediaCompleteEvent) {
							System.out
									.println(String
											.format("[%s] Checking if event operation [%s] is same as [%s].",
													DateFormatUtils.format(
															new Date(),
															"hh:mm:ss.SSS"),
													((MohoMediaCompleteEvent) event)
															.getMediaOperation(),
													operation));
							if (((MohoMediaCompleteEvent) event)
									.getMediaOperation() == operation) {
								System.out.println(String.format(
										"[%s] Found match [%s].",
										DateFormatUtils.format(new Date(),
												"hh:mm:ss.SSS"), (T) event));
								evt = (T) event;
								break;
							}
						}
					}
				}
				System.out
						.println(String.format(
								"[%s] Checking if [%s] is null.",
								DateFormatUtils.format(new Date(),
										"hh:mm:ss.SSS"), evt));
				if (evt != null) {
					System.out.println(String.format(
							"[%s] Removing [%s] from events.",
							DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
							evt));
					events.remove(evt);
					System.out.println(String.format("[%s] Returning [%s].",
							DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
							evt));
					return evt;
				}
				i++;
			}
			if (i < retries) {
				waitForEvents(waitTime);
			}
		} while (i < retries);
		throw new AssertionError("Call Event not found");
	}

	protected boolean assertNotReceived(Class eventClass,
			MediaOperation operation) {

		try {
			System.out
					.println(String
							.format("[%s] Asserting that event [%s] was not received for media operation [%s].",
									DateFormatUtils.format(new Date(),
											"hh:mm:ss.SSS"), eventClass,
									operation));
			assertReceived(eventClass, operation, 0);
		} catch (AssertionError e) {
			System.out.println(String.format("[%s] Assertion error: [%s].",
					DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
					e.getMessage()));
			return true;
		}
		throw new AssertionError("Call Event found and was not expected");
	}

	protected <T> T assertReceived(Class<T> eventClass, Call call) {

		return assertReceived(eventClass, call, retries);
	}

	protected <T> T assertReceived(Class<T> eventClass, Call call, int retries) {

		int i = 0;
		do {
			synchronized (events) {
				T evt = null;
				System.out.println(String.format(
						"[%s] Asserting event [%s] on call [%s]. Try %s",
						DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
						eventClass, call.getId(), i + 1));
				for (Event event : events) {
					if (eventClass.isAssignableFrom(event.getClass())) {
						if (event.getSource() == call) {
							evt = (T) event;
							break;
						}
					}
				}
				if (evt != null) {
					events.remove(evt);
					return evt;
				}
				i++;
			}
			waitForEvents(waitTime);
		} while (i < retries);
		System.out.println("Call event not found");
		throw new AssertionError("Call Event not found");
	}

	protected boolean assertNotReceived(Class eventClass, Call call) {

		try {
			assertReceived(eventClass, call, 0);
			throw new AssertionError("Call Event found");
		} catch (AssertionError e) {
			return true;
		}
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

	void addCall(IncomingCall call) {

		System.out.println(String.format("[%s] Adding incoming call [%s]",
				DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"), call));
		callsQueue.add(call);
	}

	void addEvent(Event event) {

		System.out.println(String.format("[%s] Adding event [%s]",
				DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"), event));
		synchronized (events) {
			events.add(event);
		}
	}

	protected AudibleResource resolveAudio(final Ssml item) {

		return new AudibleResource() {
			public URI toURI() {
				return item.toUri();
			}
		};
	}

	protected void assertCdrExists(String id) throws Exception {

		assertJmxNodeContains("com.rayo:Type=Cdrs", "ActiveCDRs", "callId", id);
	}

	protected String getCdrTranscript(String id) throws Exception {

		JmxClient jmx = new JmxClient("localhost", "8080");
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
		return "";
	}

	protected void assertJmxNodeContains(String url, String node,
			String attribute, String value) throws Exception {

		JmxClient jmx = new JmxClient("localhost", "8080");
		Object jmxObject = jmx.jmxValue(url, node);
		if (jmxObject instanceof JSONArray) {
			JSONArray cdrs = (JSONArray) jmx.jmxValue(url, node);
			boolean found = false;
			Iterator<JSONObject> it = cdrs.iterator();
			while (it.hasNext()) {
				JSONObject object = it.next();
				if (object.get(attribute).equals(value)) {
					return;
				}
			}
			fail(String
					.format("Could not find attribute [%s] with value [%s] on JSON Array at Url [%s]",
							attribute, value, url));
		} else {
			JSONObject jsonObject = (JSONObject) jmxObject;
			if (jsonObject.get(attribute) != null) {
				if (jsonObject.get(attribute).equals(value)) {
					return;
				} else {
					fail(String.format("Expected attribute [%s]. Found [%s]",
							value, jsonObject.get(attribute)));
				}
			} else {
				fail(String.format("Could not find attribute [%s] on [%s]",
						attribute, url));
			}
		}
	}

	protected String getJmxValue(String url, String node, String attribute)
			throws Exception {

		JmxClient jmx = new JmxClient("localhost", "8080");
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

	public String getXmppUsername() {
		return xmppUsername;
	}

	public void setXmppUsername(String xmppUsername) {
		this.xmppUsername = xmppUsername;
	}

	public String getXmppPassword() {
		return xmppPassword;
	}

	public void setXmppPassword(String xmppPassword) {
		this.xmppPassword = xmppPassword;
	}

	public String getSipDialUri() {

		String uri = sipDialUris.get(next);
		next = (next+1);
		if (next > sipDialUris.size()) {
			next = 0;
		}
		return uri;
	}



	public String getXmppServer() {
		return xmppServer;
	}

	public void setXmppServer(String xmppServer) {
		this.xmppServer = xmppServer;
	}

	public String getRayoServer() {
		return rayoServer;
	}

	public void setRayoServer(String rayoServer) {
		this.rayoServer = rayoServer;
	}
}
