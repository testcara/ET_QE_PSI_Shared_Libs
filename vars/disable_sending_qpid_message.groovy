def call(String token, String etPod){
  echo "---> set 'messages_to_qpid_enabled => false' on the pod ${etPod}..."
  def config_file = "/opt/app-root/src/config/initializers/settings.rb"
  def disable_qpid_cmd = "sed -i 's/messages_to_qpid_enabled => true/messages_to_qpid_enabled => false/g' ${config_file}"
  run_cmd_against_pod(token, etPod, disable_qpid_cmd)
}
