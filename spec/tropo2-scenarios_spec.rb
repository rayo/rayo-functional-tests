require 'spec_helper'

describe "Call Scenarios" do
  describe "Incoming call transferred in parallel" do
    before do
      # 1. A company receives a call in one of the virtual numbers (1.800.555.1212)
      add_latch :customer_hanging_up, :employee1_hanging_up
      place_call_with_script customer_script

      # 2. One number service answers the call and plays an announcement (selected from a predefined set or from the recordings made by user)
      get_call_and_answer
      call_output = @call.output(:ssml => audio_ssml(:url => @config['audio_url'])).should have_executed_correctly

      # 3. While the announcement is being played the call is transferred to N employees in parallel (all the employees’ phones ring in parallel)
      @tropo1.script_content = employee_script

      @employee1 = @tropo2.dial(tropo1_dial_options).should have_dialed_correctly
      @employee2 = @tropo2.dial(tropo1_dial_options).should have_dialed_correctly
      @employee3 = @tropo2.dial(tropo1_dial_options).should have_dialed_correctly

      # 4. The possible answers for each of the phones are reject, busy, timeout (no answer), cancelled (connFu hangs that call before any other answer) or accepted
      #
      # Based on the above dial commands the client will start receiving progress events for all three calls.

      # 5. One of them takes the call (employee1) [If none of them takes the call then play an announcement (selected from a predefined set or from the recordings made by user) and clear the call]
      @employee1.next_event.should be_a_valid_answered_event

      # 6. All other pending legs with employees are hung up
      @employee2.hangup.should have_executed_correctly
      @employee3.hangup.should have_executed_correctly

      # 7. The call is established end to end between the customer and employee1
      call_output.stop!.should have_executed_correctly
      call_output.next_event.should be_a_valid_stopped_output_event

      # Join employee1 to the customer
      @employee1.join(:other_call_id => @call.call_id).should have_executed_correctly
      @call.next_event.should be_a_valid_joined_event.with_other_call_id(@employee1.call_id)
      @employee1.next_event.should be_a_valid_joined_event.with_other_call_id(@call.call_id)
    end

    describe "8.1. The customer hangs up" do
      let :customer_script do
        <<-CALL_SCRIPT
          call_tropo2
          sleep 5
        CALL_SCRIPT
      end

      let :employee_script do
        <<-SCRIPT_CONTENT
          answer
          sleep 15
        SCRIPT_CONTENT
      end

      it ", and we hangup employee1" do
        @call.next_event.should be_a_valid_unjoined_event.with_other_call_id(@employee1.call_id)
        @call.next_event.should be_a_valid_hangup_event

        @employee1.next_event.should be_a_valid_unjoined_event.with_other_call_id(@call.call_id)

        hangup_and_confirm @employee1
      end
    end

    describe "8.2 The employee hangs up" do
      let :customer_script do
        <<-CALL_SCRIPT
          call_tropo2
          sleep 15
        CALL_SCRIPT
      end

      let :employee_script do
        <<-SCRIPT_CONTENT
          answer
          sleep 5
        SCRIPT_CONTENT
      end

      it ", and we hangup the customer" do
        @employee1.next_event.should be_a_valid_unjoined_event.with_other_call_id(@call.call_id)
        @employee1.next_event.should be_a_valid_hangup_event

        @call.next_event.should be_a_valid_unjoined_event.with_other_call_id(@employee1.call_id)
        hangup_and_confirm @call
      end
    end

    after :each do
      @call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should == true
      @employee1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should == true
    end
  end

  describe "Transfer of an established call triggered by DTMFs" do
    before do
      # 1. A company receives a call in one of the virtual numbers
      place_call_with_script <<-CALL_SCRIPT
        # Customer script
        call_tropo2
        wait_to_hangup 4
      CALL_SCRIPT
      get_call_and_answer

      # 2. The call is established end to end between the customer and one of the employees (employee1)
      #
      # Here is a simplified example. For a complete multi-party dial example see the use case above.
      @tropo1.script_content = <<-SCRIPT_CONTENT
        # Employee1 Script
        answer
        sleep_for_media_assertion
        play_dtmf 1
        wait_to_hangup 4
      SCRIPT_CONTENT

      @employee1 = @tropo2.dial(tropo1_dial_options).should have_dialed_correctly

      @employee1.next_event.should be_a_valid_answered_event

      @employee1.join(:other_call_id => @call.call_id).should have_executed_correctly
      @employee1.next_event.should be_a_valid_joined_event.with_other_call_id(@call.call_id)
      @call.next_event.should be_a_valid_joined_event.with_other_call_id(@employee1.call_id)

      # 3. employee1 enters a DTMF sequence (eg. 1)
      input1 = @employee1.input(:grammar => { :value => '1' }).should have_executed_correctly

      input1.next_event.should be_a_valid_successful_input_event.with_interpretation('1')

      # 4. The caller (the customer) is transferred to a new destination (employee2) while listening some music on hold and the call with employee1 is automatically hung up

      # Hangup employee1 (resulting in customer being unjoined)
      hangup_and_confirm @employee1 do
        @employee1.next_event.should be_a_valid_unjoined_event.with_other_call_id(@call.call_id)
      end
      @call.next_event.should be_a_valid_unjoined_event.with_other_call_id(@employee1.call_id)

      @employee1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should == true

      # Play Announcement
      @call_output = @call.output(:ssml => audio_ssml(:url => @config['audio_url'])).should have_executed_correctly

      # Dial ‘employee2’
      @tropo1.script_content = employee2_script
      @employee2 = @tropo2.dial(tropo1_dial_options).should have_dialed_correctly
    end

    describe "5.1 If employee2 takes the call" do
      let :employee2_script do
        <<-SCRIPT_CONTENT
          # Employee2 Script
          answer
          wait_to_hangup 4
        SCRIPT_CONTENT
      end

      it "then the call is established between the customer and employee1" do
        @employee2.next_event.should be_a_valid_answered_event
        @call_output.stop!.should have_executed_correctly
        @call_output.next_event.should be_a_valid_stopped_output_event

        @employee2.join(:other_call_id => @call.call_id).should have_executed_correctly
        @call.next_event.should be_a_valid_joined_event.with_other_call_id(@employee2.call_id)
        @employee2.next_event.should be_a_valid_joined_event.with_other_call_id(@call.call_id)

        hangup_and_confirm do
          @call.next_event.should be_a_valid_unjoined_event.with_other_call_id(@employee2.call_id)
        end

        @employee2.next_event.should be_a_valid_unjoined_event.with_other_call_id(@call.call_id)
        hangup_and_confirm @employee2
      end
    end

    describe "5.2. If employee2 didn't take the call" do
      let :employee2_script do
        <<-SCRIPT_CONTENT
          # Employee2 Script
          reject
          wait_to_hangup
        SCRIPT_CONTENT
      end

      it "then play an announcement (selected from a predefined set or from the recordings made by user) and clear the call" do
        @employee2.next_event.should be_a_valid_reject_event
        @call_output.stop!.should have_executed_correctly
        @call_output.next_event.should be_a_valid_stopped_output_event

        output = @call.output(:ssml => audio_ssml(:url => @config['audio_url'])).should have_executed_correctly
        output.next_event.should be_a_valid_output_event

        hangup_and_confirm
      end
    end

    after :each do
      @employee2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should == true
    end
  end
end
