require 'spec_helper'

describe "Conference command" do
  it "should put one caller in conference and then hangup" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    conference = @call.conference(:name => '1234', :event_callback => lambda { |event| event.is_a?(Punchblock::Component::Tropo::Conference::Speaking) || event.is_a?(Punchblock::Component::Tropo::Conference::FinishedSpeaking) }).should have_executed_correctly

    conference.next_event.should be_a_valid_conference_offhold_event

    hangup_and_confirm do
      conference.next_event.should be_a_valid_complete_hangup_event
    end
  end

  it "should allow terminating a conference" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      sleep 2
      play_dtmf '#'
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    conference = @call.conference(:name => '1234', :event_callback => lambda { |event| event.is_a?(Punchblock::Component::Tropo::Conference::Speaking) || event.is_a?(Punchblock::Component::Tropo::Conference::FinishedSpeaking) }).should have_executed_correctly

    conference.next_event.should be_a_valid_conference_offhold_event
    conference.next_event.should be_a_valid_conference_complete_terminator_event

    hangup_and_confirm
  end

  describe "with two callers in a conference" do
    it "should ensure there is media flow between the calls and that appropriate active speaker events are received" do
      pending
      add_latch :responded

      place_call_with_script <<-SCRIPT_CONTENT
        call_rayo
        ask 'One5', :choices     => 'yes, no',
                    :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                    :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      trigger_latch :responded
      wait_to_hangup
      SCRIPT_CONTENT

      @call_1 = @rayo.get_call
      @call_1.call_event.should be_a_valid_offer_event
      @call_1.answer.should have_executed_correctly
      @conference1 = @call_1.conference(:name => '1234', :event_callback => lambda { |event| event.is_a?(Punchblock::Component::Tropo::Conference::Speaking) || event.is_a?(Punchblock::Component::Tropo::Conference::FinishedSpeaking) }).should have_executed_correctly
      @conference1.next_event.should be_a_valid_conference_offhold_event

      place_call_with_script <<-SCRIPT_CONTENT
        call_rayo
        say 'yes'
        wait_to_hangup
      SCRIPT_CONTENT

      @call_2 = @rayo.get_call
      @call_2.call_event.should be_a_valid_offer_event
      @call_2.answer.should have_executed_correctly
      @conference2 = @call_2.conference(:name => '1234', :event_callback => lambda { |event| event.is_a?(Punchblock::Component::Tropo::Conference::Speaking) || event.is_a?(Punchblock::Component::Tropo::Conference::FinishedSpeaking) }).should have_executed_correctly
      @conference2.next_event.should be_a_valid_conference_offhold_event

      wait_on_latch :responded
      @tropo1.result.should == 'yes'

      @conference1.next_event.should be_a_valid_speaking_event#.for_call_id(@call_2.call_id)
      @conference1.next_event.should be_a_valid_finished_speaking_event#.for_call_id(@call_2.call_id)

      @call_1.hangup.should have_executed_correctly
      @conference1.next_event.should be_a_valid_complete_hangup_event
      @call_1.next_event.should be_a_valid_hangup_event

      @call_2.hangup.should have_executed_correctly
      @conference2.next_event.should be_a_valid_complete_hangup_event
      @call_2.next_event.should be_a_valid_hangup_event

      @call_1.last_event?(@config['rayo_queue']['last_stanza_timeout']).should == true
      @call_2.last_event?(@config['rayo_queue']['last_stanza_timeout']).should == true
    end

    it "should destroy the conference once the last participant leaves" do
      pending
      script = <<-SCRIPT_CONTENT
        call_rayo
        wait_to_hangup
      SCRIPT_CONTENT

      original_mixer_count = active_mixer_count

      place_call_with_script script

      @call_1 = @rayo.get_call
      @call_1.call_event.should be_a_valid_offer_event
      @call_1.answer.should have_executed_correctly
      @conference1 = @call_1.conference(:name => '1234', :moderator => true).should have_executed_correctly
      @conference1.next_event.should be_a_valid_conference_offhold_event

      active_mixer_count.should == original_mixer_count + 1

      place_call_with_script script

      @call_2 = @rayo.get_call
      @call_2.call_event.should be_a_valid_offer_event
      @call_2.answer.should have_executed_correctly
      @conference2 = @call_2.conference(:name => '1234', :moderator => true).should have_executed_correctly
      @conference2.next_event.should be_a_valid_conference_offhold_event

      active_mixer_count.should == original_mixer_count + 1

      @call_1.hangup.should have_executed_correctly
      @conference1.next_event.should be_a_valid_complete_hangup_event
      @call_1.next_event.should be_a_valid_hangup_event

      active_mixer_count.should == original_mixer_count + 1

      @call_2.hangup.should have_executed_correctly
      @conference2.next_event.should be_a_valid_complete_hangup_event
      @call_2.next_event.should be_a_valid_hangup_event

      active_mixer_count.should == original_mixer_count

      @call_1.last_event?(@config['rayo_queue']['last_stanza_timeout']).should == true
      @call_2.last_event?(@config['rayo_queue']['last_stanza_timeout']).should == true
    end
  end
end
