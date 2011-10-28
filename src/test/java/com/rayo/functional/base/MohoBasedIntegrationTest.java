package com.rayo.functional.base;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.common.event.MohoMediaCompleteEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.MediaOperation;
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
	
	@Before
	public void setup() {
		
		callsQueue.clear();
		events.clear();
		incomingCalls.clear();
		outgoingCalls.clear();
		
	    mohoRemote = new MohoRemoteImpl();
	    mohoRemote.addObserver(new MohoObserver(this));
	    
	    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost", "localhost");

	}
	
	@After
	public void shutdown() {
		
		try {
			for (OutgoingCall call: outgoingCalls) {
				try {
					call.disconnect();
				} catch (Exception e) {
					System.out.println(String.format("[ERROR] Problem disconnecting outgoing call [%s]", call.getId()));
				}
			}			
			
			mohoRemote.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public OutgoingCall dial() {
		
	    CallableEndpoint endpoint = (CallableEndpoint)mohoRemote.createEndpoint(URI.create("sip:usera@127.0.0.1:5060"));
	    Call call = endpoint.createCall("sip:test@test.com");
	    call.addObserver(new MohoObserver(this));
	    call.join();
	    
	    outgoingCalls.add((OutgoingCall)call);
	    
	    return (OutgoingCall)call;
	}
	
	protected synchronized IncomingCall getIncomingCall() {

		try {
			IncomingCall call = callsQueue.poll(5000,TimeUnit.MILLISECONDS);
			if (call != null) {
				incomingCalls.add(call);
			}
			return call;
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	protected <T> T assertReceived(Class<T> eventClass, MediaOperation operation) {
		
		int i = 0;
		do {
			System.out.println(String.format("Asserting event [%s] on operation [%s]. Try %s", eventClass, operation, i+1));
			for (Event event: events) {
				if (eventClass.isAssignableFrom(event.getClass())) {
					if (event instanceof MohoMediaCompleteEvent) {
						if (((MohoMediaCompleteEvent)event).getMediaOperation() == operation) {
							return (T)event;
						}
					}
				}
			}
			i++;
			waitForEvents(waitTime);
		} while (i<retries);
		throw new AssertionError("Call Event not found");
	}
	
	
	protected <T> T assertReceived(Class<T> eventClass, Call call) {
		
		int i = 0;
		do {
			System.out.println(String.format("Asserting event [%s] on call [%s]. Try %s", eventClass, call.getId(), i+1));
			for (Event event: events) {
				if (eventClass.isAssignableFrom(event.getClass())) {
					if (event.getSource() == call) {
						return (T)event;
					}
				}
			}
			i++;
			waitForEvents(waitTime);
		} while (i<retries);
		System.out.println("Call event not found");
		throw new AssertionError("Call Event not found");
	}

	private void waitForEvents(int time) {
		 
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {

		}
	}

	void addCall(IncomingCall call) {

		System.out.println(String.format("Adding incoming call [%s]",call));
		callsQueue.add(call);
	}

	void addEvent(Event event) {

		System.out.println(String.format("Adding event [%s]",event));
		events.add(event);
	}
}
