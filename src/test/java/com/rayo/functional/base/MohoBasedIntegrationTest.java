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

public abstract class MohoBasedIntegrationTest implements Observer {

	private LinkedBlockingQueue<IncomingCall> callsQueue = new LinkedBlockingQueue<IncomingCall>();
	private List<Event> events = new ArrayList<Event>();

	private MohoRemote mohoRemote;
	
	private int retries = 3;
	private int waitTime = 3000;
	
	@Before
	public void setup() {
		
		callsQueue.clear();
		events.clear();
		
	    mohoRemote = new MohoRemoteImpl();
	    mohoRemote.addObserver(this);
	    
	    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "127.0.0.1", "127.0.0.1");

	}
	
	@After
	public void shutdown() {
		
		try {
			mohoRemote.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public OutgoingCall dial() {
		
	    CallableEndpoint endpoint = (CallableEndpoint)mohoRemote.createEndpoint(URI.create("sip:usera@127.0.0.1:5060"));
	    Call call = endpoint.createCall("sip:test@test.com");
	    call.addObserver(this);
	    call.join();
	    
	    return (OutgoingCall)call;
	}
	
	protected IncomingCall getIncomingCall() {

		try {
			return callsQueue.poll(5000,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	@State
	public void handleEvent(Event event) {
		
		if (event instanceof IncomingCall) {
			
			IncomingCall call = (IncomingCall)event;
			call.addObserver(this);
			callsQueue.add(call);
		} else {
			events.add(event);
		}
	}
	
	protected <T> T assertReceived(Class<T> eventClass, MediaOperation operation) {
		
		System.out.println(String.format("Asserting event [%s] on operation [%s]", eventClass, operation));
		for (Event event: events) {
			if (eventClass.isAssignableFrom(event.getClass())) {
				if (event instanceof MohoMediaCompleteEvent) {
					if (((MohoMediaCompleteEvent)event).getMediaOperation() == operation) {
						return (T)event;
					}
				}
			}
		}
		throw new AssertionError("Call Event not found");
	}
	
	
	protected <T> T assertReceived(Class<T> eventClass, Call call) {
		
		System.out.println(String.format("Asserting event [%s] on call [%s]", eventClass, call.getId()));
		int i = 0;
		do {
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
}
