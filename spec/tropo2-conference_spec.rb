require 'spec_helper'

describe "Conference command" do
  it "should put one caller in conference and then hangup" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.conference(:name => '1234').should be_true

    @call.next_event.should be_a_valid_conference_command
    @call.next_event.should be_a_valid_joined_event.with_mixer_id('1234')

    hangup_and_confirm do
      @call.next_event.should be_a_valid_unjoined_event.with_mixer_id('1234')
      @call.next_event.should be_a_valid_complete_hangup_event
    end
  end

  it "should put two callers into a conference and then hangup" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    SCRIPT_CONTENT

    call_1 = @tropo2.get_call
    call_1.call_event.should be_a_valid_offer_event
    call_1.answer.should be_true
    call_1.conference(:name => '1234').should be_true
    call_1.next_event.should be_a_valid_conference_command
    call_1.next_event.should be_a_valid_joined_event.with_mixer_id('1234')

    @tropo1.place_call @config['tropo1']['session_url']

    call_2 = @tropo2.get_call
    call_2.call_event.should be_a_valid_offer_event
    call_2.answer.should be_true
    call_2.conference(:name => '1234').should be_true
    call_2.next_event.should be_a_valid_conference_command
    call_2.next_event.should be_a_valid_joined_event.with_mixer_id('1234')

    call_1.hangup.should be_true
    call_1.next_event.should be_a_valid_unjoined_event.with_mixer_id('1234')
    call_1.next_event.should be_a_valid_complete_hangup_event
    call_1.next_event.should be_a_valid_hangup_event

    call_2.hangup.should be_true
    call_2.next_event.should be_a_valid_unjoined_event.with_mixer_id('1234')
    call_2.next_event.should be_a_valid_complete_hangup_event
    call_2.next_event.should be_a_valid_hangup_event

    call_1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
    call_2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end

  it "should put two callers into a conference, validate media and hangup" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      ask 'One5', :choices     => 'yes, no',
                 :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                 :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      wait_to_hangup
    SCRIPT_CONTENT

    call_1 = @tropo2.get_call
    call_1.call_event.should be_a_valid_offer_event
    call_1.answer.should be_true
    call_1.conference(:name => '1234').should be_true
    call_1.next_event.should be_a_valid_conference_command
    call_1.next_event.should be_a_valid_joined_event.with_mixer_id('1234')

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      say 'yes'
      wait_to_hangup
    SCRIPT_CONTENT

    call_2 = @tropo2.get_call
    call_2.call_event.should be_a_valid_offer_event
    call_2.answer.should be_true
    call_2.conference(:name => '1234').should be_true
    call_2.next_event.should be_a_valid_conference_command
    call_2.next_event.should be_a_valid_joined_event.with_mixer_id('1234')

    sleep @config['media_assertion_timeout'] + 2

    call_1.hangup.should be_true
    call_1.next_event.should be_a_valid_unjoined_event.with_mixer_id('1234')
    call_1.next_event.should be_a_valid_complete_hangup_event
    call_1.next_event.should be_a_valid_hangup_event

    call_2.hangup.should be_true
    call_2.next_event.should be_a_valid_unjoined_event.with_mixer_id('1234')
    call_2.next_event.should be_a_valid_complete_hangup_event
    call_2.next_event.should be_a_valid_hangup_event

    @tropo1.result.should == 'yes'

    call_1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
    call_2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true
  end
end
