def call(String perf_username, String perf_user_token, String et_build_name_or_id, String perf_expect_run_time, String baseline_job_id, String et_server='errata-stage-perf.host.stage.eng.bos.redhat.com'){
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
        sh "echo $perf_username  > perf_username"
        sh "echo $perf_user_token > perf_user_token"
        sh "echo $et_build_name_or_id > et_build_name_or_id"
        sh "echo $perf_expect_run_time > perf_expect_run_time"
        sh "echo $baseline_job_id > baseline_job_id"
        sh '''
          whoami || true
          psi-jenkins-slave
          whoami

          source /etc/bashrc
          export perf_username=$(cat perf_username)
          export perf_user_token=$(cat perf_user_token)
          export et_build_name_or_id=$(cat et_build_name_or_id)
          export perf_expect_run_time=$(cat perf_expect_run_time)
          export baseline_job_id=$(cat baseline_job_id)

          echo "===============Download the CI files under $(pwd)=========="
          wget http://github.com/testcara/RC_CI/archive/master.zip
          unzip master.zip
          source RC_CI-master/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh
          source RC_CI-master/auto_testing_CI/CI_Shell_common_usage.sh

          et_build_version=""
          install_scripts_env
          if [[ ${et_build_name_or_id} == "" ]]
          then
            echo "If there is no exact version specified, let us get the version directly from the target server."
            et_build_name_or_id=$(curl http://${et_server}/system_version.txt)
          fi
          et_build_version=$(initial_et_build_version ${et_build_name_or_id})

          cd RC_CI-master/auto_testing_CI
          echo "=== Trigger performance testing ==="
          python talk_to_perf_jenkins.py full_perf ${perf_expect_run_time} ${perf_username} ${perf_user_token} ${baseline_job_id} ${et_build_version}
          '''
        } //container
    } // node
  } //pod
} //call
