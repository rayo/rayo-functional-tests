require 'rubygems'
require 'bundler'

begin
  Bundler.setup(:default, :development)
rescue Bundler::BundlerError => e
  $stderr.puts e.message
  $stderr.puts "Run `bundle install` to install missing gems"
  exit e.status_code
end

require 'rspec/core'
require 'rspec/core/rake_task'
require 'ci/reporter/rake/rspec'

task :hudson => ["ci:setup:rspec", :dial, :redirect, :jmx, :cdr, :reject, :say, :ask, :conference, :'answer-hangup', :dtmf, :transfer, :record]

RSpec::Core::RakeTask.new(:rspec) do |spec|
  mapper          = { "junit" => "JUnitFormatter", "tap" => "TapFormatter" }
  format          = mapper[ENV["format"]] || "progress"
  spec.rspec_opts = ["-r lib/j_unit_formatter.rb", "-r lib/tap_formatter.rb", "-f \"#{format}\""]
  spec.pattern    = "spec/**/*_spec.rb"
end

RSpec::Core::RakeTask.new(:spec) do |spec|
  spec.pattern = FileList['spec/**/*_spec.rb']
end

%w{answer-hangup ask conference dial dtmf jmx cdr record redirect reject transfer say}.each do |command|
  RSpec::Core::RakeTask.new(command.to_sym) do |spec|
    spec.pattern = FileList["spec/**/tropo2-#{command}_spec.rb"]
  end
end

task :default => :rspec

