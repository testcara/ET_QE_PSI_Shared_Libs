def call(String et_server, String et_version, String errata_fetch_brew_build='false', String get_the_target_build='false', String dev_jenkins_user='', String dev_jenkins_user_token='', String dev_jenkins_job=''){
  def runner = "mypod-${UUID.randomUUID().toString()}"
  podTemplate(label: runner,
  containers: [
  containerTemplate(
    name: 'et-python2-runner',
    image: 'docker-registry.upshift.redhat.com/errata-qe-test/et_python2_runner:latest',
    alwaysPullImage: true,
    command: 'cat',
    ttyEnabled: true,
    envVars: [
      envVar(key: 'GIT_SSL_NO_VERIFY', value: 'true')
    ]

    )]
  )
  {
    node(runner) {
      container('et-python2-runner'){
        sh "echo $et_server > et_server"
        sh "echo $et_version > et_version"
        sh "echo $errata_fetch_brew_build > errata_fetch_brew_build"
        sh "echo $get_the_target_build > get_the_target_build"
        sh "echo $dev_jenkins_job > dev_jenkins_job"
        sh "echo $dev_jenkins_user > dev_jenkins_user"
        sh "echo $dev_jenkins_user_token > dev_jenkins_user_token"
        try {
          sh '''
            whoami || true
            psi-jenkins-slave
            whoami

            export dev_jenkins_user=$(cat dev_jenkins_user)
            export dev_jenkins_user_token=$(cat dev_jenkins_user_token)
            export ET_Testing_Server=$(cat et_server)
            export et_build_name_or_id=$(cat et_version)
            export errata_fetch_brew_build=$(cat errata_fetch_brew_build)
            export get_the_target_build=$(cat get_the_target_build)
            export dev_jenkins_job=$(cat dev_jenkins_job)

            git config --global http.sslVerify false
            git clone https://gitlab.infra.prod.eng.rdu2.redhat.com/ansible-playbooks/errata-tool-playbooks.git
            wget http://github.com/testcara/RC_CI/archive/master.zip
            unzip master.zip
            export WORKSPACE=`pwd`
            export PYTHONHTTPSVERIFY=0

            source RC_CI-master/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh
            source RC_CI-master/auto_testing_CI/CI_Shell_common_usage.sh
            install_scripts_env

            if [[ "${get_the_target_build}" == "true" ]]
            then
              et_build_id_and_branch=$(python RC_CI-master/auto_testing_CI/talk_to_rc_jenkins_to_get_the_target_build.py ${dev_jenkins_user} ${dev_jenkins_user_token} ${dev_jenkins_job})
              echo "${et_build_id_and_branch}" | cut -d " " -f 1 > et_build_name_or_id
              echo "${et_build_id_and_branch}" | cut -d " " -f 3 > et_branch
            fi

            RC_CI-master/auto_testing_CI/prepare_et_server_with_rpm_by_ansible.sh
          '''
        }
        catch (Exception e){ 
          sh "echo env.BUILD_URL > test_report_url"
          sh '''
          et_build_name_or_id=$(cat et_build_name_or_id)
          branch=$(cat et_branch)
          test_report_url=$(cat test_report_url)
          echo "ET Version/Commit: ${et_build_name_or_id} of branch ${branch}"
          echo "Testing Result: FAILED
          echo "Testing Report URL: ${test_report_url}"
          '''
        } //catch  
      } //container
    } // node
  } //pod
} //call
