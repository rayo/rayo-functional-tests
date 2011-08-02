require 'spec_helper'

describe "Join command" do
  before { pending }

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
        call1_joined = calls[0].next_event
        call1_joined.should be_a_valid_joined_event
        call1_joined.other_call_id.should == calls[1].call_id

        call2_joined = calls[1].next_event
        call2_joined.should be_a_valid_joined_event
        call2_joined.other_call_id.should == calls[0].call_id

        calls.each { |call| hangup_and_confirm call }
      end
    end

    describe "to a mixer" do
      def join(opts = {})
        @call.join({:mixer_id => mixer_id}.merge(opts)).should be_true
      end

      let(:mixer_id) { 'abc123' }

      before :each do
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
        joined = @call.next_event
        joined.should be_a_valid_joined_event
        joined.mixer_id.should == mixer_id

        hangup_and_confirm
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

      call1_joined = calls[0].next_event
      call1_joined.should be_a_valid_joined_event
      call1_joined.other_call_id.should == calls[1].call_id

      call2_joined = calls[1].next_event
      call2_joined.should be_a_valid_joined_event
      call2_joined.other_call_id.should == calls[0].call_id

      calls[0].unjoin(:other_call_id => calls[1].call_id).should be_true

      call1_unjoined = calls[0].next_event
      call1_unjoined.should be_a_valid_unjoined_event
      call1_unjoined.other_call_id.should == calls[1].call_id

      call2_unjoined = calls[1].next_event
      call2_unjoined.should be_a_valid_unjoined_event
      call2_unjoined.other_call_id.should == calls[0].call_id

      calls.each { |call| hangup_and_confirm call }
    end

    it "from a mixer" do
      place_call_with_script simple_script
      get_call_and_answer

      mixer_id = 'abc123'

      @call.join(:mixer_id => mixer_id).should be_true

      joined = @call.next_event
      joined.should be_a_valid_joined_event
      joined.mixer_id.should == mixer_id

      @call.unjoin(:mixer_id => mixer_id).should be_true

      unjoined = @call.next_event
      unjoined.should be_a_valid_unjoined_event
      unjoined.mixer_id.should == mixer_id

      hangup_and_confirm
    end
  end

  describe "a nested join" do
    describe "can join a call" do
      describe "to another call" do
        it "in receive mode"

        it "in send mode"

        it "in duplex mode"

        it "with direct media"

        it "with bridged media"
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
        it "in receive mode"

        it "in send mode"

        it "in duplex mode"

        it "with direct media"

        it "with bridged media"
      end
    end
  end
end
