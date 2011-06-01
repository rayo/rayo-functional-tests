require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Transfer verb" do
    it "Should answer a call and then transfer it" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      @tropo2.read_event_queue.should be_a_valid_call_event
      @tropo2.answer.should eql true
    
      # Set a script that handles the incoming Xfer from Tropo2
      @tropo1.script_content = <<-SCRIPT_CONTENT
        answer
        wait 1000
        hangup
      SCRIPT_CONTENT
    
      @tropo2.transfer(@config['tropo1']['call_destination']).should eql true
      @tropo2.read_event_queue.should be_a_valid_transfer_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should try to transfer but get a timeout" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      @tropo2.read_event_queue.should be_a_valid_call_event
      @tropo2.answer.should eql true
    
      # Set a script that handles the incoming Xfer from Tropo2
      @tropo1.script_content = <<-SCRIPT_CONTENT
        wait 5000
      SCRIPT_CONTENT
    
      @tropo2.transfer(@config['tropo1']['call_destination'], :timeout => 2000).should eql true
      @tropo2.read_event_queue.should be_a_valid_transfer_timeout_event
    end
  end
end