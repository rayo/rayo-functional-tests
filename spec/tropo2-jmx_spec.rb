require File.expand_path(File.dirname(__FILE__) + '/spec_helper')
require 'net/http'
require 'json'

describe "Tropo2AutomatedFunctionalTesting" do
  describe "JMX Tests" do
        
    it "Should find JMX MBeans available" do

    	server = @config['tropo2_server']['server']
    	port = @config['tropo2_server']['port'].to_i
    	res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Info', port)
		res.code.should eql '200'
    end

    it "Should find Build Number" do
    
    	server = @config['tropo2_server']['server']
    	port = @config['tropo2_server']['port'].to_i
    	res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Info', port)

		json = JSON.parse res.body

		json['value']['BuildNumber'].should_not eql nil
    end

    it "Should find all main JMX Beans" do
    
 		server = @config['tropo2_server']['server']
 		port = @config['tropo2_server']['port'].to_i
    	res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Info', port)
    	res.code.should eql '200'

    	res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', port)
    	res.code.should eql '200'

    	res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Admin', port)
    	res.code.should eql '200'

    	res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Calls', port)
    	res.code.should eql '200'

    	res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Ozone', port)
    	res.code.should eql '200'

    end

    it "Be able to enable quiesce mode" do
    
		server = @config['tropo2_server']['server']
		port = @config['tropo2_server']['port'].to_i
		res = Net::HTTP.get_response(server, '/tropo2/jmx/exec/com.tropo:Type=Admin/enableQuiesce', port)
		res.code.should eql '200'
		json = JSON.parse res.body
		json['error'].should eql nil
    end
        
    it "Be able to disable quiesce mode" do
    
		server = @config['tropo2_server']['server']
		port = @config['tropo2_server']['port'].to_i
		res = Net::HTTP.get_response(server, '/tropo2/jmx/exec/com.tropo:Type=Admin/disableQuiesce', port)
		res.code.should eql '200'
		json = JSON.parse res.body
		json['error'].should eql nil
    end 
            
    it "Does process incoming calls" do
    
		server = @config['tropo2_server']['server']
		port = @config['tropo2_server']['port'].to_i
		res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', port)
		json = JSON.parse res.body
		calls = json['value']['IncomingCalls'].to_i
		
        @tropo1.script_content = <<-SCRIPT_CONTENT
          call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        SCRIPT_CONTENT
        @tropo1.place_call @config['tropo1']['session_url']
  				
		sleep(10)
		
		res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', port)
		json = JSON.parse res.body
		calls2 = json['value']['IncomingCalls'].to_i
		calls2.should eql calls+1
    end 

    it "Do not accept calls on Quiesce enabled" do
    
    	server = @config['tropo2_server']['server']
    	port = @config['tropo2_server']['port'].to_i
		begin
			res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', port)
			json = JSON.parse res.body
			calls = json['value']['CallsRejected'].to_i
			
			Net::HTTP.get_response(server, '/tropo2/jmx/exec/com.tropo:Type=Admin/enableQuiesce', port)

	        @tropo1.script_content = <<-SCRIPT_CONTENT
	          call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
	        SCRIPT_CONTENT
	        @tropo1.place_call @config['tropo1']['session_url']
      				
			sleep(6)
			
			res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Call%20Statistics', port)
			json = JSON.parse res.body
			calls2 = json['value']['CallsRejected'].to_i
			calls2.should eql calls+1
		ensure
			Net::HTTP.get_response(server, '/tropo2/jmx/exec/com.tropo:Type=Admin/disableQuiesce', port)
		end
    end  
  end
end