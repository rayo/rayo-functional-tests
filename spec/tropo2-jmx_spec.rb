require File.expand_path(File.dirname(__FILE__) + '/spec_helper')
require 'net/http'
require 'json'

describe "Tropo2AutomatedFunctionalTesting" do
  describe "JMX Tests" do
        
    it "Should find JMX MBeans available" do
    	res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Info', 8080)
		res.code.should eql '200'
    end

    it "Should find Build Number" do
    	res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Info', 8080)

		json = JSON.parse res.body

		json['value']['BuildNumber'].should_not eql nil
    end

    it "Should find all main JMX Beans" do
    	res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Info', 8080)
    	res.code.should eql '200'

    	res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', 8080)
    	res.code.should eql '200'

    	res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Admin', 8080)
    	res.code.should eql '200'

    	res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Calls', 8080)
    	res.code.should eql '200'

    	res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Ozone', 8080)
    	res.code.should eql '200'

    end

    it "Be able to enable quiesce mode" do

		res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/exec/com.tropo:Type=Admin/enableQuiesce', 8080)
		res.code.should eql '200'
		json = JSON.parse res.body
		json['error'].should eql nil
    end
        
    it "Be able to disable quiesce mode" do

		res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/exec/com.tropo:Type=Admin/disableQuiesce', 8080)
		res.code.should eql '200'
		json = JSON.parse res.body
		json['error'].should eql nil
    end 
            
    it "Does process incoming calls" do

		res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', 8080)
		puts res
		puts 'Quiesce mode'
		puts Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Admin', 8080)
		json = JSON.parse res.body
		calls = json['value']['IncomingCalls'].to_i
		
        @tropo1.script_content = <<-SCRIPT_CONTENT
          call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        SCRIPT_CONTENT
        @tropo1.place_call @config['tropo1']['session_url']
  				
		sleep(10)
		
		res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', 8080)
		puts res
		json = JSON.parse res.body
		calls2 = json['value']['IncomingCalls'].to_i
		calls2.should eql calls+1
    end 

    it "Do not accept calls on Quiesce enabled" do
		begin
			res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', 8080)
			json = JSON.parse res.body
			calls = json['value']['CallsRejected'].to_i
			
			Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/exec/com.tropo:Type=Admin/enableQuiesce', 8080)

	        @tropo1.script_content = <<-SCRIPT_CONTENT
	          call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
	        SCRIPT_CONTENT
	        @tropo1.place_call @config['tropo1']['session_url']
      				
			sleep(6)
			
			res = Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', 8080)
			json = JSON.parse res.body
			calls2 = json['value']['CallsRejected'].to_i
			calls2.should eql calls+1
		ensure
			Net::HTTP.get_response('127.0.0.1', '/tropo2/jmx/exec/com.tropo:Type=Admin/disableQuiesce', 8080)
		end
    end  
  end
end