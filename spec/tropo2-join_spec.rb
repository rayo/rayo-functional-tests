require 'spec_helper'

describe "Join command" do
  let :simple_script do
    <<-SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    SCRIPT_CONTENT
  end

  describe "can join a call" do
    describe "to another call" do
      let :calls do
        [].tap do |calls|
          2.times do
            place_call_with_script simple_script
            calls << get_call_and_answer
          end
        end
      end

      def join(opts = {})
        calls[0].join({:other_call_id => calls[1].call_id}.merge(opts)).should be_true
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

    describe "to a mixer" do
      def join(opts = {})
        @call.join({:mixer_id => mixer_id}.merge(opts)).should be_true
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

      calls[0].join(:other_call_id => calls[1].call_id).should be_true

      calls[0].next_event.should be_a_valid_joined_event.with_other_call_id(calls[1].call_id)
      calls[1].next_event.should be_a_valid_joined_event.with_other_call_id(calls[0].call_id)

      calls[0].unjoin(:other_call_id => calls[1].call_id).should be_true

      calls[0].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[1].call_id)
      calls[1].next_event.should be_a_valid_unjoined_event.with_other_call_id(calls[0].call_id)

      calls.each { |call| hangup_and_confirm call }
    end

    it "from a mixer" do
      pending
      place_call_with_script simple_script
      get_call_and_answer

      mixer_id = 'abc123'

      @call.join(:mixer_id => mixer_id).should be_true

      @call.next_event.should be_a_valid_joined_event.with_mixer_id(mixer_id)

      @call.unjoin(:mixer_id => mixer_id).should be_true

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

        let :dial_options do
          {
            :to      => @config['tropo1']['call_destination'],
            :from    => 'tel:+14155551212',
            :headers => { 'x-tropo2-drb-address' => @drb_server_uri }
          }
        end

        def dial_join(opts = {})
          @call2 = @tropo2.dial(dial_options.merge(:join => { :other_call_id => @call.call_id }.merge(opts))).should be_true
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
          @call2.ring_event.should be_a_valid_ringing_event
          @call2.next_event.should be_a_valid_answered_event
          @call2.next_event.should be_a_valid_joined_event.with_other_call_id(@call.call_id)

          @call.next_event.should be_a_valid_joined_event.with_other_call_id(@call2.call_id)

          hangup_and_confirm @call do
            @call.next_event.should be_a_valid_unjoined_event.with_other_call_id(@call2.call_id)
          end
          @call2.next_event.should be_a_valid_unjoined_event.with_other_call_id(@call.call_id)
          hangup_and_confirm @call2
        end
      end

      describe "to a busy callee" do
        it "and be rejected" do
          pending
          @tropo1.script_content = 'reject'

          @call = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                               :from    => 'tel:+14155551212',
                               :headers => { 'x-tropo2-drb-address' => @drb_server_uri }
          @call.ring_event.should be_a_valid_ringing_event
          @call.next_event.should be_a_valid_reject_event
        end
      end

      describe "to a mixer" do
        let :dial_options do
          {
            :to      => @config['tropo1']['call_destination'],
            :from    => 'tel:+14155551212',
            :headers => { 'x-tropo2-drb-address' => @drb_server_uri }
          }
        end

        def dial_join(opts = {})
          @call = @tropo2.dial(dial_options.merge(:join => { :mixer_id => mixer_id }.merge(opts))).should be_true
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
          # @call.ring_event.should be_a_valid_ringing_event
          # @call.next_event.should be_a_valid_answered_event
          # @call.next_event.should be_a_valid_joined_event.with_mixer_id(mixer_id)
          # hangup_and_confirm
        end
      end
    end
  end
end
