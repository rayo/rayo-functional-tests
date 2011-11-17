package com.rayo.functional.rayoapi;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.Duration;
import org.junit.Ignore;
import org.junit.Test;

import com.rayo.core.verb.Transfer;
import com.rayo.core.verb.TransferCompleteEvent;
import com.rayo.core.verb.TransferCompleteEvent.Reason;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class TransferTest extends RayoBasedIntegrationTest {
	
	@Test
	public void testAnswerAndTransfer() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI(sipDialUri)).getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    
	    rayoClient.transfer(new URI(sipDialUri), incomingCallId);
    	String incomingCall2 = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall2);

    	rayoClient.hangup(outgoingCallId);
    	rayoClient.hangup(incomingCall2);
	}	

	@Test
	public void testCompleteSuccess() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI(sipDialUri)).getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    rayoClient.transfer(new URI(sipDialUri), incomingCall);
    	String incomingCall2 = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall2);
	    
	    rayoClient.answer(incomingCall2);
	    waitForEvents(100);
	    rayoClient.hangup(incomingCall2);
	    waitForEvents(300);
	    
	    TransferCompleteEvent complete = assertReceived(TransferCompleteEvent.class, incomingCall);
	    assertEquals(complete.getReason(), Reason.SUCCESS);

    	rayoClient.hangup(outgoingCallId);
    	rayoClient.hangup(incomingCall2);
	}	
	
	@Test
	public void testTransferTimeout() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI(sipDialUri)).getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    
	    Transfer transfer = new Transfer();
	    List<URI> uris = new ArrayList<URI>();
	    uris.add(new URI(sipDialUri));
	    transfer.setTo(uris);
	    transfer.setTimeout(new Duration(2000));
	    
	    rayoClient.transfer(transfer, incomingCallId);
    	String incomingCallId2 = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId2);	    
	    waitForEvents(2000);
	    
	    TransferCompleteEvent complete = assertReceived(TransferCompleteEvent.class, incomingCallId);
	    assertEquals(complete.getReason(), Reason.TIMEOUT);

    	rayoClient.hangup(outgoingCallId);
    	rayoClient.hangup(incomingCallId2);
	}
	
	@Test
	public void testTransferToMultipleDestinations() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI(sipDialUri)).getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    
	    Transfer transfer = new Transfer();
	    List<URI> uris = new ArrayList<URI>();
	    uris.add(new URI(sipDialUri));
	    uris.add(new URI(sipDialUri));
	    transfer.setTo(uris);
	    transfer.setTimeout(new Duration(2000));
	    
	    rayoClient.transfer(transfer, incomingCallId);
    	String incomingCallId2 = getIncomingCall().getCallId();   
    	String incomingCallId3 = getIncomingCall().getCallId();   
    	assertNotSame(incomingCallId2, incomingCallId3);
    
    	rayoClient.hangup(outgoingCallId);
    	rayoClient.hangup(incomingCallId2);
    	rayoClient.hangup(incomingCallId3);
	}
	
	@Test
	public void testTransferReject() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI(sipDialUri)).getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    Transfer transfer = new Transfer();
	    List<URI> uris = new ArrayList<URI>();
	    uris.add(new URI(sipDialUri));
	    transfer.setTo(uris);
	    rayoClient.transfer(transfer, incomingCall);
	    
    	String incomingCall2 = getIncomingCall().getCallId();
    	rayoClient.reject(incomingCall2);
    	waitForEvents(300);
    	
	    TransferCompleteEvent complete = assertReceived(TransferCompleteEvent.class, incomingCall);
	    assertEquals(complete.getReason(), Reason.REJECT);

    	rayoClient.hangup(outgoingCallId);
	}
	
	
	@Test
	@Ignore
	//TODO:  #1594500
	public void testTransferTerminator() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI(sipDialUri)).getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    Transfer transfer = new Transfer();
	    transfer.setTerminator('#');
	    List<URI> uris = new ArrayList<URI>();
	    uris.add(new URI(sipDialUri));
	    transfer.setTo(uris);
	    rayoClient.transfer(transfer, incomingCall);
	    
    	String incomingCall2 = getIncomingCall().getCallId();
    	rayoClient.answer(incomingCall2);
    	waitForEvents(300);
    	rayoClient.dtmf("#", incomingCall2);
    	waitForEvents(300);
    	
	    TransferCompleteEvent complete = assertReceived(TransferCompleteEvent.class, incomingCall);
	    assertEquals(complete.getReason(), Reason.TERMINATOR);

    	rayoClient.hangup(outgoingCallId);
	}
}