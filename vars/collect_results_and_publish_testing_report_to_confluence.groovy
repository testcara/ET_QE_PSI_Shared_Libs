def call(String psi_jenkins_username, String psi_jenkins_password, String confluence_username, String confluence_password, String dev_jenkins_username, String dev_jenkins_password, String dev_jenkins_job='', String et_build_name_or_id="", String space='PDT', String parent_page='56821226'){
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
        sh "echo $et_build_name_or_id > et_build_name_or_id"
        sh "echo $psi_jenkins_username > psi_jenkins_username"
        sh "echo $psi_jenkins_password > psi_jenkins_password"
        sh "echo $confluence_username > confluence_username"
        sh "echo $confluence_password > confluence_password"
        sh "echo $dev_jenkins_username > dev_jenkins_username"
        sh "echo $dev_jenkins_password > dev_jenkins_password"
        sh "echo $dev_jenkins_job > dev_jenkins_job"
        sh "echo $space > space"
        sh "echo $parent_page > parent_page"
        sh '''
          whoami || true
          psi-jenkins-slave
          whoami

          source /etc/bashrc
          export et_build_name_or_id=$(cat et_build_name_or_id)
          export jenkins_username=$(cat psi_jenkins_username)
          export jenkins_password=$(cat psi_jenkins_password)
          export confluence_username=$(cat confluence_username)
          export confluence_password=$(cat confluence_password)
          export dev_jenkins_username=$(cat dev_jenkins_username)
          export dev_jenkins_password=$(cat dev_jenkins_password)
          export dev_jenkins_job=$(cat dev_jenkins_job)
          export space=$(cat space)
          export parent_page=$(cat parent_page)

          echo "===============Download the CI files under $(pwd)=========="
          git clone https://gitlab.cee.redhat.com/wlin/rc_ci.git

          source rc_ci/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh
          source rc_ci/auto_testing_CI/CI_Shell_common_usage.sh

          install_scripts_env
          et_build_version=$(initial_et_build_version ${et_build_name_or_id})
          if [[ "$et_build_version" == "" ]]
          then
            et_build_version=$(python rc_ci/auto_testing_CI/talk_to_rc_jenkins_to_get_the_latest_dev_build.py ${dev_jenkins_username} ${dev_jenkins_password} ${dev_jenkins_job})
          fi

          title="ET Testing Reports For Build ${et_build_version}"
          export RC_Jenkins_URL="https://jenkins-errata-qe-test.cloud.paas.psi.redhat.com"
          echo "Collecting all results and generate the report "
          python rc_ci/auto_testing_CI/collect_all_reports_and_update_to_confluence_psi.py "${jenkins_username}" "${jenkins_password}" "${confluence_username}" "${confluence_password}" "${et_build_version}" "${title}" "${space}" "${parent_page}"

          '''
        } //container
    } // node
  } //pod
} //call
