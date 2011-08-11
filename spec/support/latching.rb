def add_latch(*latch_names)
  latch_names.each { |latch_name| @tropo1.add_latch latch_name }
end

def wait_on_latch(latch_name)
  @tropo1.wait(latch_name).should_not timeout
end

def trigger_latch(latch_name)
  @tropo1.trigger latch_name
end

RSpec::Matchers.define :timeout do
  match do |actual|
    actual.should == false
  end

  failure_message_for_should do |actual|
    "expected latch to timeout.".tap do |s|
      s << "call_id: #{@call.call_id}" if @call
    end
  end

  failure_message_for_should do |actual|
    "expected latch not to timeout.".tap do |s|
      s << "call_id: #{@call.call_id}" if @call
    end
  end

  description do
    "timeout"
  end
end
