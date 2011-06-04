require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Conference command" do
    it "Should put one caller in conference and then hangup" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event      
      call.answer.should eql true
    
      call.conference('1234').should eql true
      call.hangup.should eql true
      call.next_event.should be_a_valid_conference_event
      call.next_event.should be_a_valid_hangup_event
    
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should put two callers into a conference and then hangup" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call_1 = @tropo2.get_call
      call_1.call_event.should be_a_valid_call_event      
      call_1.answer.should eql true
      call_1.conference('1234').should eql true
      
      @tropo1.place_call @config['tropo1']['session_url']
      
      call_2 = @tropo2.get_call
      call_2.call_event.should be_a_valid_call_event      
      call_2.answer.should eql true
      call_2.conference('1234').should eql true
      
      call_1.hangup.should eql true
      call_1.next_event.should be_a_valid_conference_event
      call_1.next_event.should be_a_valid_hangup_event
    
      call_1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
      
      call_2.hangup.should eql true
      call_2.next_event.should be_a_valid_conference_event
      call_2.next_event.should be_a_valid_hangup_event
    
      call_2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should put two callers into a conference, validate media and hangup" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        ask 'One', { :choices     => 'yes, no',
                     :onBadChoice => lambda { ozone_testing_server.tropo_result = 'badchoice' },
                     :onChoice    => lambda { |event| ozone_testing_server.result = event.value  } }
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call_1 = @tropo2.get_call
      call_1.call_event.should be_a_valid_call_event      
      call_1.answer.should eql true
      call_1.conference('1234').should eql true
      
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep 2
        say 'yes'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call_2 = @tropo2.get_call
      call_2.call_event.should be_a_valid_call_event      
      call_2.answer.should eql true
      call_2.conference('1234').should eql true
      
      sleep @config['media_assertion_timeout'] + 2
      
      call_1.hangup.should eql true
      call_1.next_event.should be_a_valid_conference_event
      call_1.next_event.should be_a_valid_hangup_event
    
      call_1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
      
      call_2.hangup.should eql true
      call_2.next_event.should be_a_valid_conference_event
      call_2.next_event.should be_a_valid_hangup_event
      
      @tropo1.result.should == 'yes'
      
      call_2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end