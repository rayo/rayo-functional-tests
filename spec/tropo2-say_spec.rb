require 'spec_helper'

describe "Say command" do
  it "Should say something with TTS" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      ask 'One7', :choices     => 'yes, no',
                 :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                 :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      ozone_testing_server.trigger :responded
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.add_latch :responded
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.answer.should be_true

    call.say(:text => 'yes').should be_true

    @tropo1.wait :responded

    call.next_event.should be_a_valid_say_event

    call.hangup.should be_true
    call.next_event.should be_a_valid_hangup_event

    @tropo1.result.should eql 'yes'

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end

  it "Should say an audio URL and hangup" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.answer.should be_true

    call.say(:audio => { :url => 'http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3' }).should be_true

    sleep 9 #Wait for audio file to complete playing

    call.next_event.should be_a_valid_say_event

    call.hangup.should be_true
    call.next_event.should be_a_valid_hangup_event

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end

  it "Should say SSML" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      ask 'One8', :choices     => 'one hundred, ireland',
                 :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                 :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      ozone_testing_server.trigger :responded
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.add_latch :responded
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.answer.should be_true

    call.say(:ssml => '<say-as interpret-as="ordinal">100</say-as>').should be_true

    @tropo1.wait :responded

    call.next_event.should be_a_valid_say_event

    call.hangup.should be_true
    call.next_event.should be_a_valid_hangup_event

    @tropo1.result.should eql 'one hundred'

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end

  it "Should say some audio, wait 2 seconds, pause, wait 2 seconds, resume, wait 2 seconds and then stop" do
    pending 'https://github.com/tropo/punchblock/issues/10' do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should be_true

      say_event = call.say 'http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3', :url
      ap say_event

      sleep 2
      ap say_event.pause
      sleep 2
      ap say_event.resume
      sleep 2
      ap say_event.stop

      hangup_event = @tropo2.hangup(call_event)
      hangup_event.should be_a_valid_hangup_event

      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
    end
  end

  it "Should say an audio URL and get a stop event" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      sleep 2
      hangup
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.answer.should be_true

    call.say(:audio => { :url => 'http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3' }).should be_true

    call.next_event.should be_a_valid_complete_hangup_event
    call.next_event.should be_a_valid_hangup_event

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end

  it "Should error on a say and return a complete event" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      sleep 2
      hangup
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.answer.should be_true

    lambda { call.say :text => '' }.should raise_error(Punchblock::Protocol::ProtocolError)

    call.next_event.should be_a_valid_hangup_event

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end
end
