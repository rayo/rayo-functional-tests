def active_cdrs
  response = Net::HTTP.get_response(@config['tropo2_server']['server'], '/tropo2/jmx/read/com.tropo:Type=Cdrs/ActiveCDRs', @config['tropo2_server']['port'].to_i)
  @active_cdrs ||= JSON.parse(response.body)['value']
end

def check_cdr_is_current_call
  active_cdrs.should have(1).record
  active_cdrs.first['callId'].should eql @call.call_event.call_id
end
