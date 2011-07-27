require 'spec_helper'

describe "Ask command" do
  let :grxml do
    <<-GRXML
      <grammar xmlns="http://www.w3.org/2001/06/grammar" root="MAINRULE">
        <rule id="MAINRULE">
          <one-of>
            <item>
              <item repeat="0-1"> need a</item>
              <item repeat="0-1"> i need a</item>
              <one-of>
                <item> clue </item>
              </one-of>
              <tag> out.concept = "clue";</tag>
            </item>
            <item>
              <item repeat="0-1"> have an</item>
              <item repeat="0-1"> i have an</item>
              <one-of>
                <item> answer </item>
              </one-of>
              <tag> out.concept = "answer";</tag>
            </item>
          </one-of>
        </rule>
      </grammar>
    GRXML
  end

  it "should ask something with ASR and get the utterance back" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      sleep #{@config['media_assertion_timeout']}.to_i
      say 'yes'
      ozone_testing_server.trigger :responded
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.add_latch :responded
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true

    @call.ask(:prompt  => { :text  => 'One1' },
              :choices => { :value => 'yes, no' }).should be_true

    @tropo1.wait :responded

    ask_event = @call.next_event
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.utterance.should eql 'yes'

    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should ask something with DTMF and get the interpretation back" do
    pending 'Tropo2 does not currently support in-band DTMF' do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep #{@config['media_assertion_timeout']}.to_i
        say '#{@config['dtmf_tone_files'][3]}'
        ozone_testing_server.trigger :responded
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.add_latch :responded
      @tropo1.place_call @config['tropo1']['session_url']

      @call = @tropo2.get_call
      @call.call_event.should be_a_valid_call_event
      @call.answer.should be_true

      @call.ask(:prompt  => { :text  => 'One2' },
                :choices => { :value => '[1 DIGITS]' },
                :mode    => :dtmf).should be_true

      @tropo1.wait :responded

      ask_event = @call.next_event 2
      ask_event.should be_a_valid_successful_ask_event
      ask_event.reason.interpretation.should eql '3'

      @call.hangup.should be_true
      @call.next_event.should be_a_valid_hangup_event
    end
  end

  it "should ask with an SSML as a prompt" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      sleep #{@config['media_assertion_timeout']}.to_i
      say 'yes'
      ozone_testing_server.trigger :responded
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.add_latch :responded
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true

    @call.ask(:prompt  => { :text  => '<say-as interpret-as="ordinal">100</say-as>' },
              :choices => { :value => 'yes, no' }).should be_true

    @tropo1.wait :responded

    ask_event = @call.next_event
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.utterance.should eql 'yes'

    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should ask with a GRXML grammar" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      sleep 3
      say 'clue'
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true

    @call.ask(:prompt  => { :text         => 'One3' },
              :choices => { :value        =>  grxml,
                            :content_type => 'application/grammar+grxml' } ).should be_true

    ask_event = @call.next_event
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.utterance.should eql 'clue'

    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should ask with an SSML prompt and a GRXML grammar" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      sleep 1
      ask 'clue', :choices     => 'one hundred, ireland',
                  :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                  :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true

    @call.ask(:prompt  => { :text  => '<say-as interpret-as="ordinal">100</say-as>' },
              :choices => { :value => grxml,
                            :content_type => 'application/grammar+grxml' } ).should be_true

    ask_event = @call.next_event
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.utterance.should eql 'clue'

    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should ask and get a NOINPUT event" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait 5000
      hangup
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true

    @call.ask :prompt  => { :text  => 'Yeap' },
              :choices => { :value => 'yes, no' },
              :timeout => 2000

    @call.next_event.should be_a_valid_noinput_event
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should ask and get a NOMATCH event with min_confidence set to 1" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait 1000
      say 'blue'
      wait #{@config['tropo1']['wait_to_hangup']}
      hangup
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true

    @call.ask :prompt         => { :text  => 'Yeap' },
              :choices        => { :value => 'red, green' },
              :timeout        => 3000,
              :min_confidence => 1

    @call.next_event.should be_a_valid_nomatch_event

    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should ask and get a STOP if the farside hangs up before the command complete" do
    pending 'https://github.com/tropo/tropo2/issues/59' do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait 8000
        hangup
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      @call = @tropo2.get_call
      @call.call_event.should be_a_valid_call_event
      @call.answer.should be_true

      @call.ask :prompt  => { :text  => 'Yeap' },
                :choices => { :value => 'red, green' }

      @call.next_event.should be_a_valid_stopped_ask_event
      @call.next_event.should be_a_valid_hangup_event
    end
  end

  it "should ask something with an invalid grammar and get an error back" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait 2000
      hangup
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true

    lambda { @call.ask :prompt  => { :text => 'One4' },
                       :choices => { :value => '<grammar>' } }.should raise_error(Punchblock::Protocol::ProtocolError)

    @call.next_event.reason.should eql :error
  end
end
