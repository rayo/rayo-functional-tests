require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Transfer verb" do
    it "Should answer a call and then transfer it" do
      pending('https://github.com/tropo/tropo2/issues/53')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
    
      # Set a script that handles the incoming Xfer from Tropo2
      @tropo1.script_content = <<-SCRIPT_CONTENT
        answer
        wait 1000
        hangup
      SCRIPT_CONTENT
    
      call.transfer(:to      => @config['tropo1']['call_destination'],
                    :headers => { 'x-tropo2-drb-address' => @config['tropo2_server']['drb_server_address'] }).should eql true
                    
      call.next_event.should be_a_valid_transfer_event
      call.next_event.should be_a_valid_hangup_event
      
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should try to transfer but get a timeout" do
      pending('https://github.com/tropo/tropo2/issues/53')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
    
      # Set a script that handles the incoming Xfer from Tropo2
      @tropo1.script_content = <<-SCRIPT_CONTENT
        wait 5000
      SCRIPT_CONTENT
    
      call.transfer(:to      => @config['tropo1']['call_destination'], 
                    :timeout => 2000,
                    :headers => { 'x-tropo2-drb-address' => @config['tropo2_server']['drb_server_address'] }).should eql true

      call.next_event.should be_a_valid_transfer_timeout_event
      
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end