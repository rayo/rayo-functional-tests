require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Conference verb" do
  it "Should put one caller in conference and then hangup" do
    pending('https://github.com/tropo/tropo2/issues/14')
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']
    
    call_event = @tropo2.read_event_queue
    call_event.should be_a_valid_call_event
      
    answer_event = @tropo2.answer
    answer_event.should be_a_valid_answer_event
    
    conference_event = @tropo2.conference '1234'
    ap conference_event
    
    hangup_event = @tropo2.hangup
    hangup_event.should be_a_valid_hangup_event
    
    @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
  end
  
  it "Should put two callers into a conference and then hangup" do
    pending('https://github.com/tropo/tropo2/issues/14')
  end
  
  it "Should put two callers into a conference, validate media and hangup" do
    pending('https://github.com/tropo/tropo2/issues/14')
  end
end