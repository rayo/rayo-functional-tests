require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Dial command" do
    it "Should place an outbound call, receive a ring event, receive an answer event and then hangup" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        sleep 2
        hangup
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
    
      @tropo2.dial(:to => @config['tropo1']['call_destination'], :from => 'tel:+14155551212').should eql true
      @tropo2.read_event_queue.should be_a_valid_ring_event
      @tropo2.read_event_queue.should be_a_valid_answer_event
      @tropo2.read_event_queue.should be_a_valid_hangup_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
        
    it "Should place an outbound call and then receive a reject event" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        reject
      TROPO_SCRIPT_CONTENT
    
      @tropo2.dial(:to => @config['tropo1']['call_destination'], :from => 'tel:+14155551212').should eql true
      @tropo2.read_event_queue.should be_a_valid_ring_event
      @tropo2.read_event_queue.should be_a_valid_reject_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end