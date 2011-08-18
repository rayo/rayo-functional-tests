def hangup_and_confirm(call = @call)
  call.hangup.should have_executed_correctly
  yield if block_given?
  call.next_event.should be_a_valid_hangup_event
end

def get_call_and_answer(answer = true)
  @call = @tropo2.get_call
  @call.call_event.should be_a_valid_offer_event
  @call.answer.should have_executed_correctly if answer
  @call
end
