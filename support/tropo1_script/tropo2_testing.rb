require 'drb/drb'

drb_server_address = $drb_server_address || $currentCall.getHeader('x-tropo2-drb-address')

log "====>Connecting to the Ozone Test Server @ #{drb_server_address}<===="

begin
  # Connect to the DRb server on the RSpec instance
  ozone_testing_server = DRbObject.new_with_uri drb_server_address

  log "====>Connected with the Ozone Test Server<===="

  script_content = ozone_testing_server.script_content
  log "====>Eval Script:" + script_content

  eval script_content
rescue => e
  log "====>Error: #{e.to_s}<===="
end
