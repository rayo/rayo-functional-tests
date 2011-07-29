require 'net/http'
require 'json'

def call_jmx(operation, params)
  Net::HTTP.get_response server, "/tropo2/jmx/#{operation}/com.tropo:#{params}", port
end

def jmx_read(params)
  call_jmx 'read', params
end

def jmx_exec(params)
  call_jmx 'exec', params
end

def call_statistics
  JSON.parse jmx_read('Type=Call%20Statistics').body
end

def try_call
  @tropo1.add_latch :tropo1_finished
  place_call_with_script <<-SCRIPT_CONTENT
    call_tropo2
    trigger_latch :tropo1_finished
  SCRIPT_CONTENT

  @tropo1.wait :tropo1_finished
end
