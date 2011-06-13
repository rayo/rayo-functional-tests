require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Redirect command" do
    it "Should redirect a call" do
      pending('https://github.com/tropo/punchblock/issues/22')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
  
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
  
      # Send an answer to Tropo2
      call.redirect @config['tropo1']['call_destination'].should eql true
      ap call.next_event
      ap call.next_event
      ap call.next_event
      #redirect_event.should be_a_valid_answer_event
  
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end