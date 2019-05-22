def call(String token, String etPod){
  echo "---> Change the umb setting..."
  def umb_config_1 = '/opt/app-root/src/config/initializers/credentials/message_bus.rb'
  def umb_config_2 = '/opt/app-root/src/examples/ruby/message_bus/umb_configuration.rb'
  [ umb_config_1, umb_config_2 ].each {
    def change_umb_cmd="sed -i \"s/ENV\\['ET_UMB_BROKER_URL_1'\\]/'amqp:\\/\\/cucumber-umb-qe:5672'/g\" ${it}"
    run_cmd_against_pod(token, etPod, change_umb_cmd)
    ef change_umb_cmd="sed -i \"s/ENV\\['ET_UMB_BROKER_URL_2'\\]/'amqp:\\/\\/cucumber-umb-qe:5672'/g\" ${it}"
    run_cmd_against_pod(token, etPod, change_umb_cmd)
  }
}
