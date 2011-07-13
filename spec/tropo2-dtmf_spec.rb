require 'spec_helper'

describe "Tropo2AutomatedFunctionalTesting" do
  describe "DTMF events" do
    it "Should be generated when DTMF tones are detected" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say '#{@config['dtmf_tone_files']['3']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true

      dtmf_event = call.next_event
      dtmf_event.should be_a_valid_dtmf_event
      dtmf_event.signal.should == '3'

      call.hangup.should eql true
      call.next_event.should be_a_valid_hangup_event

      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end
