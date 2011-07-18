require 'spec_helper'

describe "DTMF events" do
  it "Should be generated when DTMF tones are detected" do
    pending "Currently need a running <ask/>" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say '#{@config['dtmf_tone_files'][3]}'
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

  it "Should be generated when DTMF tones are detected during an <ask/>" do
    pending 'Tropo2 does not currently support in-band DTMF' do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep #{@config['media_assertion_timeout']}.to_i
        say '#{@config['dtmf_tone_files'][3]}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true

      call.ask(:prompt  => { :text  => 'Three?' },
               :choices => { :value => '[1 DIGITS]' },
               :mode    => :dtmf).should eql true

      sleep @config['media_assertion_timeout']

      ask_event = call.next_event
      p ask_event
      ask_event.should be_a_valid_ask_event
      ask_event.reason.utterance.should eql '3'

      dtmf_event = call.next_event
      dtmf_event.should be_a_valid_dtmf_event
      dtmf_event.signal.should == '3'

      call.hangup.should eql true
      call.next_event.should be_a_valid_hangup_event

      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end

  it "should send DTMF tones correctly" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      ask 'One', :choices     => '[1 DIGITS]',
                 :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                 :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.answer.should eql true

    call.say(:audio => { :url => 'dtmf:5' }).should eql true

    sleep @config['media_assertion_timeout']

    call.next_event.should be_a_valid_say_event

    call.hangup.should eql true
    call.next_event.should be_a_valid_hangup_event

    @tropo1.result.should eql '5'

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
  end
end
