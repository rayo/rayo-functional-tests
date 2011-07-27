require 'spec_helper'

describe "Conference command" do
  it "should put one caller in conference and then hangup" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.answer.should be_true

    call.conference(:name => '1234').should be_true
    call.hangup.should be_true
    call.next_event.should be_a_valid_conference_command
    call.next_event.should be_a_valid_complete_hangup_event
    call.next_event.should be_a_valid_hangup_event

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end

  it "should put two callers into a conference and then hangup" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call_1 = @tropo2.get_call
    call_1.call_event.should be_a_valid_call_event
    call_1.answer.should be_true
    call_1.conference(:name => '1234').should be_true

    @tropo1.place_call @config['tropo1']['session_url']

    call_2 = @tropo2.get_call
    call_2.call_event.should be_a_valid_call_event
    call_2.answer.should be_true
    call_2.conference(:name => '1234').should be_true

    call_1.hangup.should be_true
    call_1.next_event.should be_a_valid_conference_command
    call_1.next_event.should be_a_valid_complete_hangup_event
    call_1.next_event.should be_a_valid_hangup_event

    call_2.hangup.should be_true
    call_2.next_event.should be_a_valid_conference_command
    call_2.next_event.should be_a_valid_complete_hangup_event
    call_2.next_event.should be_a_valid_hangup_event

    call_1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
    call_2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end

  it "should put two callers into a conference, validate media and hangup" do
    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      ask 'One5', :choices     => 'yes, no',
                 :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                 :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call_1 = @tropo2.get_call
    call_1.call_event.should be_a_valid_call_event
    call_1.answer.should be_true
    call_1.conference(:name => '1234').should be_true

    @tropo1.script_content = <<-SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      say 'yes'
      wait #{@config['tropo1']['wait_to_hangup']}
    SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call_2 = @tropo2.get_call
    call_2.call_event.should be_a_valid_call_event
    call_2.answer.should be_true
    call_2.conference(:name => '1234').should be_true

    sleep @config['media_assertion_timeout'] + 2

    call_1.hangup.should be_true
    call_1.next_event.should be_a_valid_conference_command
    call_1.next_event.should be_a_valid_complete_hangup_event
    call_1.next_event.should be_a_valid_hangup_event

    call_2.hangup.should be_true
    call_2.next_event.should be_a_valid_conference_command
    call_2.next_event.should be_a_valid_complete_hangup_event
    call_2.next_event.should be_a_valid_hangup_event

    @tropo1.result.should == 'yes'

    call_1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
    call_2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end
end
