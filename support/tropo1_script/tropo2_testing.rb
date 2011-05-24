require 'drb/drb'

log "====>Connecting to the Ozone Test Server @ #{$server_uri}<===="

begin
  # Connect to the DRb server on the RSpec instance
  ozone_testing_server = DRbObject.new_with_uri("druby://#{$server_uri}")

  log "====>Connected with the Ozone Test Server<===="

  script_content = ozone_testing_server.script_content  
  log "====>Eval Script:" + script_content
  
  eval script_content
rescue => e
  log "====>Error: #{e.to_s}<===="
end
