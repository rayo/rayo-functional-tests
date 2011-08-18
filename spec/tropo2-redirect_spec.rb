require 'spec_helper'

describe "Redirect command" do
  it "should redirect a call" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      say 'Hello world'
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false

    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      answer
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    @call.redirect(:to => @config['tropo1']['call_destination']).should have_executed_correctly
    @call.next_event.should be_a_valid_redirect_event
  end
end
