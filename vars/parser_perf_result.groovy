def call(String jenkins_username, String jenkins_user_token, String et_build_name_or_id, String performance_tolerance, String perf_jmeter_slave_server, String max_accepted_time){
  def runner = "mypod-${UUID.randomUUID().toString()}"
  podTemplate(label: runner,
  containers: [
  containerTemplate(
    name: 'et-ansible-runner',
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
      container('et-ansible-runner'){
        sh "echo $jenkins_username  > jenkins_username"
        sh "echo $jenkins_user_token > jenkins_user_token"
        sh "echo $et_build_name_or_id > et_build_name_or_id"
        sh "echo $performance_tolerance > performance_tolerance"
        sh "echo $perf_jmeter_slave_server > perf_jmeter_slave_server"
        sh "echo $max_accepted_time > max_accepted_time"
        sh "echo env.BUILD_URL.split('/')[2].split(':')[0] > jenkins_url"
        sh '''
          whoami || true
          psi-jenkins-slave
          whoami

          source /etc/bashrc
          export username=$(cat jenkins_username)
          export password=$(cat jenkins_user_token)
          export et_build_name_or_id=$(cat et_build_name_or_id)
          export tolerance=$(cat performance_tolerance)
          export perf_jmeter_slave_server=$(cat perf_jmeter_slave_server)
          export max_accepted_time=$(cat max_accepted_time)
          export RC_Jenkins_URL=$(cat jenkins_url)

          echo "===============Download the CI files under $(pwd)=========="
          wget http://github.com/testcara/RC_CI/archive/master.zip
          unzip master.zip
          source RC_CI-master/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh
          source RC_CI-master/auto_testing_CI/CI_Shell_common_usage.sh

          et_build_version=""
          install_scripts_env
          et_build_version=$(initial_et_build_version ${et_build_name_or_id})

          cd RC_CI-master/auto_testing_CI
          echo "=== Parser performance report ==="
          python talk_to_rc_jenkins_to_parser_perf_report.py ${username} ${password} ${et_build_version} ${tolerance} ${max_accepted_time} ${perf_jmeter_slave_server}
          '''
        } //container
    } // node
  } //pod
} //call
