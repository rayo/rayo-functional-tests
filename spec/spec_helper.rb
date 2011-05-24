$LOAD_PATH.unshift(File.join(File.dirname(__FILE__), '..', 'lib'))
$LOAD_PATH.unshift(File.dirname(__FILE__))
require 'timeout'
require 'rspec'
require 'yaml'
require 'thread'
require 'awesome_print'
require 'drb'
require 'punchblock'
require 'rspec-tropo2'

Thread.abort_on_exception = true

RSpec.configure do |config|
end