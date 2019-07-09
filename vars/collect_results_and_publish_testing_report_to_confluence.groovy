def call(String et_build_name_or_id, String jenkins_username, String jenkins_password, String confluence_username, String confluence_password, String space='PDT', String parent_page='56821226'){
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
        sh "echo $jenkins_username > jenkins_username"
        sh "echo $jenkins_password > jenkins_password"
        sh "echo $confluence_username > confluence_username"
        sh "echo $confluence_password > confluence_password"
        sh "echo $space > space"
        sh "echo $parent_page > parent_page"
        sh '''
          whoami || true
          psi-jenkins-slave
          whoami

          source /etc/bashrc
          export et_build_name_or_id=$(cat et_build_name_or_id)
          export jenkins_username=$(cat jenkins_username)
          export jenkins_password=$(cat jenkins_password)
          export confluence_username=$(cat confluence_username)
          export confluence_password=$(cat confluence_password)
          export space=$(cat space)
          export parent_page=$(cat parent_page)
          export RC_Jenkins_URL="https://jenkins-errata-qe-test.cloud.paas.psi.redhat.com"

          echo "===============Download the CI files under $(pwd)=========="
          wget http://github.com/testcara/RC_CI/archive/master.zip
          unzip master.zip
          source RC_CI-master/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh
          source RC_CI-master/auto_testing_CI/CI_Shell_common_usage.sh

          et_build_version=""
          install_scripts_env
          et_build_version=$(initial_et_build_version ${et_build_name_or_id})
          title="ET Testing Reports For Build ${et_build_version}"

          echo "Collecting all results and generate the report "
          python RC_CI-master/auto_testing_CI/collect_all_reports_and_update_to_confluence_psi.py "${jenkins_username}" "${jenkins_password}" "${confluence_username}" "${confluence_password}" "${et_build_version}" "${title}" "${space}" "${parent_page}"

          '''
        } //container
    } // node
  } //pod
} //call
