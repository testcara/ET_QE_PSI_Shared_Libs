def call(String et_version, String errata_fetch_brew_build='false', String dev_jenkins_user='', String dev_jenkins_user_token='', String dev_jenkins_job='',
String perf_username, String perf_user_token, String perf_expect_run_time, String baseline_job_id, String baseline_job_name, String et_server='errata-stage-perf.host.stage.eng.bos.redhat.com'){
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
        sh "echo $errata_fetch_brew_build > errata_fetch_brew_build"
        sh "echo $dev_jenkins_job > dev_jenkins_job"
        sh "echo $dev_jenkins_user > dev_jenkins_user"
        sh "echo $dev_jenkins_user_token > dev_jenkins_user_token"
        sh "echo $perf_username  > perf_username"
        sh "echo $perf_user_token > perf_user_token"
        sh "echo $et_server > et_server"
        sh "echo $et_version > et_version"
        sh "echo $perf_expect_run_time > perf_expect_run_time"
        sh "echo $baseline_job_name > baseline_job_name"
        sh "echo $baseline_job_id > baseline_job_id"
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
            export dev_jenkins_job=$(cat dev_jenkins_job)
            git config --global http.sslVerify false
            git clone https://gitlab.infra.prod.eng.rdu2.redhat.com/ansible-playbooks/errata-tool-playbooks.git
            git clone https://gitlab.cee.redhat.com/wlin/rc_ci.git

            export WORKSPACE=`pwd`
            export PYTHONHTTPSVERIFY=0
            source rc_ci/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh
            source rc_ci/auto_testing_CI/CI_Shell_common_usage.sh
            install_scripts_env
            if [[ -z "${et_version}" ]]
            then
              et_build_id_and_branch=$(python rc_ci/auto_testing_CI/talk_to_rc_jenkins_to_get_the_target_build.py ${dev_jenkins_user} ${dev_jenkins_user_token} ${dev_jenkins_job})
              if [[ ${et_build_id_and_branch} =~ 'There is no [success] builds today' ]]
              then
                echo "There is no build today, Let us ignore the environment preparation."
                exit 0
              fi
              echo "${et_build_id_and_branch}" | cut -d " " -f 1 > et_build_name_or_id
              echo "${et_build_id_and_branch}" | cut -d " " -f 3 > et_branch
            fi
            source /etc/bashrc
            export perf_username=$(cat perf_username)
            export perf_user_token=$(cat perf_user_token)
            export et_build_name_or_id=$(cat et_version)
            export perf_expect_run_time=$(cat perf_expect_run_time)
            export baseline_job_name=$(cat baseline_job_name)
			export baseline_job_id=$(cat baseline_job_id)
            cd rc_ci/auto_testing_CI
            echo "=== Trigger performance testing ==="
            export PYTHONHTTPSVERIFY=0
            #python talk_to_perf_jenkins.py full_perf ${perf_expect_run_time} ${perf_username} ${perf_user_token} ${baseline_job_name} ${et_build_version} ${errata_fetch_brew_build} ${ET_Testing_Server}
            python talk_to_perf_jenkins.py full_perf ${perf_expect_run_time} ${perf_username} ${perf_user_token} ${baseline_job_id} ${baseline_job_name} ${et_build_name_or_id} ${errata_fetch_brew_build}
          '''
        }
        catch (Exception e){
          sh "echo env.BUILD_URL > test_report_url"
          sh '''
          et_build_name_or_id=$(cat et_version)
          branch=$(cat et_branch)
          test_report_url=$(cat test_report_url)
          echo "ET Version/Commit: ${et_build_name_or_id}"
          echo "Testing Result: FAILED
          echo "Testing Report URL: ${test_report_url}"
          '''
        } //catch
      } //container
    } // node
  } //pod
} //call
