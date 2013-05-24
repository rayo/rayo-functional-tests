package com.rayo.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.voxeo.rayo.client.JmxClient;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class CdrTest extends RayoBasedIntegrationTest {

	@Test
	public void testShouldCreateCdrOnIncomingCall() throws Exception {
		
	    String outgoingCall = dial().getCallId();
	    try {
		    String incomingCallId = getIncomingCall().getCallId();
		    	    
		    try {
			    Object value = null;
			    for (String node: getNodeNames()) {
					JmxClient nodeClient = new JmxClient(node, "8080","rayo/jmx");
					value = getAttributeValue(nodeClient, "com.rayo:Type=Cdrs", "ActiveCDRs", "callId", incomingCallId);
			    	if (value != null) break;
			    }
			    assertNotNull(value);
		    } finally {	    
		    	rayoClient.hangup(outgoingCall);
		    }
	    } finally {
	    	rayoClient.hangup(outgoingCall);
	    }
	}

	@Test
	public void testShouldCreateCdrOnOutgoingCall() throws Exception {
		
	    String outgoingCall = dial().getCallId();		    
	    try {
		    Object cdr = null;
		    for (String node: getNodeNames()) {
				JmxClient nodeClient = new JmxClient(node, "8080", "rayo/jmx");
				cdr = getCdr(nodeClient, outgoingCall);
		    	if (cdr != null) break;
		    }
		    assertNotNull(cdr);
	    } finally {	    
	    	rayoClient.hangup(outgoingCall);
	    }
	}
	
	@Test
	public void testShouldCreateTranscript() throws Exception {
		
	    String outgoingCall = dial().getCallId();		    
	    String incomingCallId = getIncomingCall().getCallId();
	    rayoClient.answer(incomingCallId);
	    rayoClient.output("Hello", incomingCallId);
    	waitForEvents();

	    try {
		    String value = null;
		    for (String node: getNodeNames()) {
				JmxClient nodeClient = new JmxClient(node, "8080", "rayo/jmx");
				value = getCdrTranscript(nodeClient, incomingCallId);
		    	if (value != null) break;
		    }
		    assertTrue(value.contains("<offer"));
		    assertTrue(value.contains("<answer"));
		    assertTrue(value.contains("<output"));
		    assertTrue(value.contains("<complete"));
		    assertTrue(value.contains("<success"));
	    } finally {	    
	    	rayoClient.hangup(outgoingCall);
	    }
	}
}
