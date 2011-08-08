def place_call
  @tropo1.place_call(@config['tropo1']['session_url']).strip
end

def place_call_with_script(script)
  @tropo1.script_content = script
  place_call
end

def tropo1_dial_options
  {
    :to      => @config['tropo1']['call_destination'],
    :from    => 'tel:+14155551212',
    :headers => { 'x-tropo2-drb-address' => @drb_server_uri }
  }
end
