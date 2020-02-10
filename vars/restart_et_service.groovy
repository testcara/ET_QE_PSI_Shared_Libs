def call(String projectName, String etPod){
  echo "---> Restart the et services ..."
  def httpd_service = "/etc/init.d/httpd24-httpd"
  def delayed_job_service = "/etc/init.d/delayed_job"
  def messaging_service = "/etc/init.d/messaging_service"
  [ httpd_service, delayed_job_service, messaging_service ].each {
    def restart_cmd = "${it} restart"
    run_cmd_against_pod(projectName, etPod, restart_cmd)
  }
}
