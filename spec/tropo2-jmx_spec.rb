require 'spec_helper'

describe "JMX Tests" do

  let(:server)  { @config['tropo2_server']['server'] }
  let(:port)    { @config['tropo2_server']['port'].to_i }

  it "should find JMX MBeans available" do
    jmx_read('Type=Info').code.should eql '200'
  end

  it "should find Build Number" do
    json = JSON.parse jmx_read('Type=Info').body
    json['value']['BuildNumber'].should_not eql nil
  end

  it "should find all main JMX Beans" do
    %w{Type=Info Type=Call%20Statistics Type=Admin,name=Admin Type=Calls Type=Ozone}.each do |bean|
      jmx_read(bean).code.should eql '200'
    end
  end

  it "Be able to enable quiesce mode" do
    res = jmx_exec 'Type=Admin,name=Admin/enableQuiesce'
    res.code.should eql '200'
    json = JSON.parse res.body
    json['error'].should eql nil
  end

  it "Be able to disable quiesce mode" do
    res = jmx_exec 'Type=Admin,name=Admin/disableQuiesce'
    res.code.should eql '200'
    json = JSON.parse res.body
    json['error'].should eql nil
  end

  it "Does process incoming calls" do
    calls_before = call_statistics['value']['IncomingCalls'].to_i

    try_call

    call_statistics['value']['IncomingCalls'].to_i.should == calls_before + 1

    get_call_and_answer # Just to clean up the pending call
  end

  it "Do not accept calls on Quiesce enabled" do
    begin
      calls_before = call_statistics['value']['CallsRejected'].to_i

      jmx_exec 'Type=Admin,name=Admin/enableQuiesce'

      try_call

      call_statistics['value']['CallsRejected'].to_i.should == calls_before + 1
      active_cdrs.should have(0).records
    ensure
      jmx_exec 'Type=Admin,name=Admin/disableQuiesce'
    end
  end
end
