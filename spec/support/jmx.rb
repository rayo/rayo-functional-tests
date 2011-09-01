require 'net/http'
require 'json'

def call_jmx(operation, params, namespace = nil)
  url = "/#{@config['rayo_server']['deployed_as'] || 'rayo'}/jmx/#{operation}/#{namespace || 'com.rayo'}:#{params}"
  Net::HTTP.get_response @config['rayo_server']['server'], url, @config['rayo_server']['port'].to_i
end

def jmx_read(params, namespace = nil)
  call_jmx 'read', params, namespace
end

def jmx_exec(params, namespace = nil)
  call_jmx 'exec', params, namespace
end

def active_sessions
  JSON.parse(jmx_read('name=sip,type=server.service/ActiveSession', 'com.micromethod.sipmethod').body)['value']
end

def call_statistics
  JSON.parse jmx_read('Type=Call%20Statistics').body
end

def try_call(should_get_call = true)
  place_call_with_script <<-SCRIPT_CONTENT
    call_rayo
    wait_to_hangup
  SCRIPT_CONTENT

  if should_get_call
    get_call_and_answer
    hangup_and_confirm
  end
end
