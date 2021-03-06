require 'spec_helper'

describe "Join command" do
  let :simple_script do
    <<-SCRIPT_CONTENT
      call_rayo
      wait_to_hangup
    SCRIPT_CONTENT
  end

  describe "can join a call" do
    def join(opts = {})
      calls[0].join({:other_call_id => calls[1].call_id}.merge(opts)).should have_executed_correctly
    end

    describe "to another call" do
      let :calls do
        [].tap do |calls|
          2.times do
            place_call_with_script simple_script
            calls << get_call_and_answer
          end
        end
      end

      it "in receive mode" do
        join :direction => :recv
      end

      it "in send mode" do
        join :direction => :send
      end

      it "in duplex mode" do
        join :direction => :duplex
      end

      it "with direct media" do
        join :media => :direct
      end

      it "with bridged media" do
        join :media => :bridge
      end

      after :each do
        calls[0].next_event.should be_a_valid_joined_event.with_other_call_id(calls[1].call_id)
        calls[1].next_event.should be_a_valid_joined_event.with_other_call_id(calls[0].call_id)

        hangup_and_confirm calls[0] do
          calls[0].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[1].call_id)
        end
        calls[1].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[0].call_id)
        hangup_and_confirm calls[1]
      end
    end

    describe "to another call which hangs up" do
      let :calls do
        [].tap do |calls|
          place_call_with_script <<-SCRIPT_CONTENT
            call_rayo
            sleep 30
          SCRIPT_CONTENT
          calls << get_call_and_answer

          add_latch :call2_hanging_up
          place_call_with_script <<-SCRIPT_CONTENT
            call_rayo
            sleep 3
            trigger_latch :call2_hanging_up
          SCRIPT_CONTENT
          calls << get_call_and_answer
        end
      end

      it "gets the correct events" do
        join

        calls[0].next_event.should be_a_valid_joined_event.with_other_call_id(calls[1].call_id)
        calls[1].next_event.should be_a_valid_joined_event.with_other_call_id(calls[0].call_id)

        wait_on_latch :call2_hanging_up

        calls[1].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[0].call_id)
        calls[1].next_event.should be_a_valid_hangup_event

        calls[0].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[1].call_id)
        hangup_and_confirm calls[0]
      end
    end

    describe "to another call and hangs up on the remote end" do
      let :calls do
        [].tap do |calls|
          add_latch :call1_hanging_up
          place_call_with_script <<-SCRIPT_CONTENT
            call_rayo
            sleep 3
            trigger_latch :call1_hanging_up
          SCRIPT_CONTENT
          calls << get_call_and_answer

          place_call_with_script <<-SCRIPT_CONTENT
            call_rayo
            sleep 30
          SCRIPT_CONTENT
          calls << get_call_and_answer
        end
      end

      it "gets the correct events" do
        join

        calls[0].next_event.should be_a_valid_joined_event.with_other_call_id(calls[1].call_id)
        calls[1].next_event.should be_a_valid_joined_event.with_other_call_id(calls[0].call_id)

        wait_on_latch :call1_hanging_up

        calls[0].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[1].call_id)
        calls[0].next_event.should be_a_valid_hangup_event

        calls[1].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[0].call_id)
        hangup_and_confirm calls[1]
      end
    end

    describe "to a mixer" do
      def join(opts = {})
        @call.join({:mixer_id => mixer_id}.merge(opts)).should have_executed_correctly
      end

      let(:mixer_id) { 'abc123' }

      before :each do
        pending
        place_call_with_script simple_script
        get_call_and_answer
      end

      it "in receive mode" do
        join :direction => :recv
      end

      it "in send mode" do
        join :direction => :send
      end

      it "in duplex mode" do
        join :direction => :duplex
      end

      it "with direct media" do
        join :media => :direct
      end

      it "with bridged media" do
        join :media => :bridge
      end

      after :each do
        # @call.next_event.should be_a_valid_joined_event.with_mixer_id(mixer_id)
        # hangup_and_confirm
      end
    end
  end

  describe "can unjoin a call" do
    it "from another call" do
      calls = [].tap do |calls|
        2.times do
          place_call_with_script simple_script
          calls << get_call_and_answer
        end
      end

      calls[0].join(:other_call_id => calls[1].call_id).should have_executed_correctly

      calls[0].next_event.should be_a_valid_joined_event.with_other_call_id(calls[1].call_id)
      calls[1].next_event.should be_a_valid_joined_event.with_other_call_id(calls[0].call_id)

      calls[0].unjoin(:other_call_id => calls[1].call_id).should have_executed_correctly

      calls[0].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[1].call_id)
      calls[1].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[0].call_id)

      calls.each { |call| hangup_and_confirm call }
    end

    it "from a mixer" do
      pending
      place_call_with_script simple_script
      get_call_and_answer

      mixer_id = 'abc123'

      @call.join(:mixer_id => mixer_id).should have_executed_correctly

      @call.next_event.should be_a_valid_joined_event.with_mixer_id(mixer_id)

      @call.unjoin(:mixer_id => mixer_id).should have_executed_correctly

      @call.next_event.should be_a_valid_unjoined_event.with_mixer_id(mixer_id)

      hangup_and_confirm
    end
  end

  describe "a nested join" do
    describe "can join a call" do
      describe "to another call" do
        before do
          place_call_with_script simple_script
          get_call_and_answer
          @tropo1.script_content = <<-SCRIPT_CONTENT
            answer
            wait_to_hangup
          SCRIPT_CONTENT
        end

        def dial_join(opts = {})
          @call2 = @rayo.dial(tropo1_dial_options.merge(:join => { :other_call_id => @call.call_id }.merge(opts))).should have_dialed_correctly
        end

        it "in receive mode" do
          dial_join :direction => :recv
        end

        it "in send mode" do
          dial_join :direction => :send
        end

        it "in duplex mode" do
          dial_join :direction => :duplex
        end

        it "with direct media" do
          pending
          dial_join :media => :direct
        end

        it "with bridged media" do
          dial_join :media => :bridge
        end

        after :each do
          if @call2
            @call2.next_event.should be_a_valid_answered_event
            @call2.next_event.should be_a_valid_joined_event.with_other_call_id(@call.call_id)

            @call.next_event.should be_a_valid_joined_event.with_other_call_id(@call2.call_id)

            hangup_and_confirm @call do
              @call.next_event.should be_a_valid_unjoined_event.with_other_call_id(@call2.call_id)
            end
            @call2.next_event.should be_a_valid_unjoined_event.with_other_call_id(@call.call_id)
            hangup_and_confirm @call2
          else
            hangup_and_confirm
          end
        end
      end

      describe "to a busy callee" do
        it "and be rejected" do
          pending
          @tropo1.script_content = 'reject'

          @call = @rayo.dial(tropo1_dial_options).should have_dialed_correctly
          @call.next_event.should be_a_valid_reject_event
        end
      end

      describe "to a mixer" do
        def dial_join(opts = {})
          @call = @rayo.dial(tropo1_dial_options.merge(:join => { :mixer_id => mixer_id }.merge(opts))).should have_dialed_correctly
        end

        let(:mixer_id) { 'abc123' }

        before { pending }

        it "in receive mode" do
          dial_join :direction => :recv
        end

        it "in send mode" do
          dial_join :direction => :send
        end

        it "in duplex mode" do
          dial_join :direction => :duplex
        end

        it "with direct media" do
          dial_join :media => :direct
        end

        it "with bridged media" do
          dial_join :media => :bridge
        end

        after :each do
          # @call.next_event.should be_a_valid_answered_event
          # @call.next_event.should be_a_valid_joined_event.with_mixer_id(mixer_id)
          # hangup_and_confirm
        end
      end
    end
  end
end
