require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Dial command" do
    it "Should place an outbound call" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
    
      # Request an outbound call
      @tropo2.dial(:to => @config['tropo1']['call_destination'], :from => 'tel:+14155551212').should eql true
      @tropo2.read_event_queue.should be_a_valid_call_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should place an outbound call to multiple destinations" do
      pending('https://github.com/tropo/punchblock/issues/20')
    end
  
    it "Should place an outbound call with a Caller ID set" do
      pending('https://github.com/tropo/punchblock/issues/20')
    end
  end

  describe "Reject and redirect" do
    it "Should reject a call" do
      pending('More testing')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      # Read the Tropo2 queue and validate the offer
      call_event = @tropo2.read_event_queue
      ap call_event
      call_event.should be_a_valid_call_event
  
      # Send an answer to Tropo2
      reject_event = @tropo2.reject
      ap reject_event
      reject_event.should be_a_valid_answer_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should redirect a call" do
      pending('Additional testing')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      # Read the Tropo2 queue and validate the offer
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
    
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
    
      # Send an answer to Tropo2
      redirect_event = @tropo2.redirect @config['tropo1']['call_destination']
      ap redirect_event
      ap @tropo2.read_event_queue
      ap @tropo2.read_event_queue
      ap @tropo2.read_event_queue
      redirect_event.should be_a_valid_answer_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end