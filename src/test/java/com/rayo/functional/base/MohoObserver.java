package com.rayo.functional.base;

import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.Observer;

public class MohoObserver implements Observer {

	private MohoBasedIntegrationTest test;

	public MohoObserver(MohoBasedIntegrationTest test) {
		
		this.test = test;
	}
	
	@State
	public void handleEvent(Event event) {
		
		if (event instanceof IncomingCall) {
			
			IncomingCall call = (IncomingCall)event;
			call.addObserver(this);
			test.addCall(call);
		} else {
			test.addEvent(event);
		}
	}
}
