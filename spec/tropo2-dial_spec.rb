require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Dial command" do
    pending('Need to allow for creation of outbound calls')
    it "Should place an outbound call, receive a ring event, receive an answer event and then hangup" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        sleep 2
        hangup
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
    
      call = @tropo2.create_call
      call.dial(:to => @config['tropo1']['call_destination'], :from => 'tel:+14155551212').should eql true
      call.next_event.should be_a_valid_ring_event
      call.next_event.should be_a_valid_answer_event
      call.next_event.should be_a_valid_hangup_event
      
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
        
    it "Should place an outbound call and then receive a reject event" do
      pending('Need to allow for creation of outbound calls')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        reject
      TROPO_SCRIPT_CONTENT
    
      @tropo2.dial(:to => @config['tropo1']['call_destination'], :from => 'tel:+14155551212').should eql true
      @tropo2.read_event_queue.should be_a_valid_ring_event
      @tropo2.read_event_queue.should be_a_valid_reject_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should place an outbound call and send SIP headers" do
      pending('Need to allow for creation of outbound calls')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        ozone_testing_server.result = $currentCall.getHeader('x-tropo2-test')
        sleep 1
        hangup
      TROPO_SCRIPT_CONTENT
    
      @tropo2.dial(:to      => @config['tropo1']['call_destination'], 
                   :from    => 'tel:+14155551212',
                   :headers => { 'x-tropo2-test' => 'booyah!' } ).should eql true
      @tropo2.read_event_queue.should be_a_valid_ring_event
      @tropo2.read_event_queue.should be_a_valid_answer_event
      @tropo2.read_event_queue.should be_a_valid_hangup_event
      @tropo1.result.should eql 'booyah!'
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end