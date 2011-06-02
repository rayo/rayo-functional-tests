require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Conference command" do
    it "Should put one caller in conference and then hangup" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      @tropo2.read_event_queue.should be_a_valid_call_event      
      @tropo2.answer.should eql true
    
      @tropo2.conference('1234').should eql true
      @tropo2.read_event_queue.should eql nil # Temp based on this: https://github.com/tropo/punchblock/issues/27
      @tropo2.hangup.should eql true
      @tropo2.read_event_queue.should be_a_valid_conference_event
      @tropo2.read_event_queue.should be_a_valid_hangup_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should put two callers into a conference and then hangup" do
      pending('https://github.com/tropo/tropo2/issues/23')
    end
  
    it "Should put two callers into a conference, validate media and hangup" do
      pending('https://github.com/tropo/tropo2/issues/23')
    end
  end
end