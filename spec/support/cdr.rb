def active_cdrs(reload = false)
  @active_cdrs = nil if reload
  @active_cdrs ||= JSON.parse(jmx_read('Type=Cdrs/ActiveCDRs').body)['value']
end

def check_cdr_is_current_call
  active_cdrs.should have(1).record
  active_cdrs.first['callId'].should eql @call.call_event.call_id
end

def check_no_remaining_calls
  active_cdrs(true).should have(0).records
end
