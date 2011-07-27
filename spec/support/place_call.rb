def place_call
  @tropo1.place_call @config['tropo1']['session_url']
end

def place_call_with_script(script)
  @tropo1.script_content = script
  place_call
end
