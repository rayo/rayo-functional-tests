require 'net/http'
require 'json'

def call_jmx(operation, params)
  Net::HTTP.get_response @config['tropo2_server']['server'], "/#{@config['tropo2_server']['deployed_as'] || 'tropo2'}/jmx/#{operation}/com.tropo:#{params}", @config['tropo2_server']['port'].to_i
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
  add_latch :tropo1_finished
  place_call_with_script <<-SCRIPT_CONTENT
    call_tropo2
    trigger_latch :tropo1_finished
  SCRIPT_CONTENT

  wait_on_latch :tropo1_finished
end
