require 'spec_helper'

describe "Reject command" do
  it "should reject with a declined reason" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    @call.reject(:reason => :decline).should be_true
    @call.next_event.should be_a_valid_reject_event
  end

  it "should reject with a busy reason" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    @call.reject(:reason => :busy).should be_true
    @call.next_event.should be_a_valid_reject_event
  end

  it "should reject with a error reason" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    @call.reject(:reason => :error).should be_true
    @call.next_event.should be_a_valid_reject_event
  end

  it "should reject and raise an error due to an invalid reason" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    lambda { @call.reject :reason => :foobar }.should raise_error(ArgumentError)
    @call.reject(:reason => :busy).should be_true
    @call.next_event.should be_a_valid_reject_event
  end
end
