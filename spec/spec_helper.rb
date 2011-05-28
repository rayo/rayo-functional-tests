$LOAD_PATH.unshift(File.join(File.dirname(__FILE__), '..', 'lib'))
$LOAD_PATH.unshift(File.dirname(__FILE__))
%w{
  timeout
  rspec
  yaml
  thread
  awesome_print
  drb
  logger
  punchblock
  rspec-tropo2
  }.each { |lib| require lib }
  # /Users/jsgoecke/Dropbox/Development/punchblock/lib/punchblock
  # /Users/jsgoecke/Dropbox/Development/rspec-tropo2/lib/rspec-tropo2

Thread.abort_on_exception = true

RSpec.configure do |config|
end