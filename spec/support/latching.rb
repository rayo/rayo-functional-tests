def add_latch(*latch_names)
  latch_names.each { |latch_name| @tropo1.add_latch latch_name }
end

def wait_on_latch(latch_name)
  @tropo1.wait latch_name
end

def trigger_latch(latch_name)
  @tropo1.trigger latch_name
end
