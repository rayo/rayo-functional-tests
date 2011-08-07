require 'drb/drb'

drb_server_address = $drb_server_address ? "druby://#{$drb_server_address}" : $currentCall.getHeader('x-tropo2-drb-address')

log "====>Connecting to the Ozone Test Server @ #{drb_server_address}<===="

def call_tropo2
  call "sip:#{$ozone_testing_server.config['tropo2_server']['sip_uri']}"
end

def wait_to_hangup(times = 1)
  times.times { sleep $ozone_testing_server.config['tropo1']['wait_to_hangup'] }
end

def sleep_for_media_assertion
  sleep $ozone_testing_server.config['media_assertion_timeout'].to_i
end

def play_dtmf(digit)
  say "<speak><audio src='dtmf:#{digit}' /></speak>"
end

def trigger_latch(latch_name)
  $ozone_testing_server.trigger latch_name
end

def wait_on_latch(latch_name)
  $ozone_testing_server.wait latch_name
end

begin
  # Connect to the DRb server on the RSpec instance
  ozone_testing_server = DRbObject.new_with_uri drb_server_address
  $ozone_testing_server = ozone_testing_server

  log "====>Connected with the Ozone Test Server<===="

  script_content = ozone_testing_server.script_content
  log "====>Eval Script:" + script_content

  eval script_content
rescue => e
  log "====>Error: #{e.to_s}<===="
end
