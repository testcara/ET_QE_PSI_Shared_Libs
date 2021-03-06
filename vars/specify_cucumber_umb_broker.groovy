def call(String projectName, String etPod){
  echo "---> Change the umb setting..."
  def umb_config_1 = '/opt/app-root/src/config/initializers/credentials/message_bus.rb'
  def umb_config_2 = '/opt/app-root/src/examples/ruby/message_bus/umb_configuration.rb'
  [ umb_config_1, umb_config_2 ].each {
    def change_umb_cmd_1="sed -i \"s/ENV\\['ET_UMB_BROKER_URL_1'\\]/'amqp:\\/\\/cucumber-umb-qe:5672'/g\" ${it}"
    run_cmd_against_pod(projectName, etPod, change_umb_cmd_1)
    def change_umb_cmd_2="sed -i \"s/ENV\\['ET_UMB_BROKER_URL_2'\\]/'amqp:\\/\\/cucumber-umb-qe:5672'/g\" ${it}"
    run_cmd_against_pod(projectName, etPod, change_umb_cmd_2)
  }
  def umb_handler = "/opt/app-root/src/lib/message_bus/handler.rb"
  def delay_to_send_message_cmd="sed -i \"/messenger.send/i \\        sleep 5\" ${umb_handler}"
  run_cmd_against_pod(projectName, etPod, delay_to_send_message_cmd)
}
