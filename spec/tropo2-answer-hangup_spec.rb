require 'spec_helper'

# startCallRecording 'http://tropo-audiofiles-to-s3.heroku.com/post_audio_to_s3?file_name=ozone2_testing.wav'

describe "Call accept, answer and hangup handling" do
  it "should receive a call and then hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should answer and hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      say 'Hello world'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true
    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should throw an error if we try to answer a call that is hungup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      say 'Hello world'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true
    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event

    lambda {@call.answer}.should raise_error(Punchblock::Protocol::ProtocolError)
  end

  it "should accept and hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      say 'Hello world'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.accept.should be_true
    @call.hangup.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should answer a call and let the farside hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      sleep 1
      hangup
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    @call = @tropo2.get_call
    @call.call_event.should be_a_valid_call_event
    @call.answer.should be_true
    @call.next_event.should be_a_valid_hangup_event
  end
end
