def call(String et_build_name_or_id, String confluence_username, String confluence_password, String et_fix_version='', String space='PDT', String jira_parent_page='34489277'){
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
        sh "echo $et_build_name_or_id > et_build_name_or_id"
        sh "echo $confluence_username > confluence_username"
        sh "echo $confluence_password > confluence_password"
        sh "echo $et_fix_version > et_fix_version"
        sh "echo $space > space"
        sh "echo $jira_parent_page > jira_parent_page"
        sh '''
          whoami || true
          ci-3-jenkins-slave
          whoami

          export et_build_name_or_id=$(cat et_build_name_or_id)
          export confluence_username=$(cat confluence_username)
          export confluence_password=$(cat confluence_password)
          export et_fix_version=$(cat et_fix_version)
          export space=$(cat space)
          export jira_parent_page=$(cat jira_parent_page)

          echo "===============Download the CI files under $(pwd)=========="
          wget http://github.com/testcara/RC_CI/archive/master.zip
          unzip master.zip
          source RC_CI-master/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh
          source RC_CI-master/auto_testing_CI/CI_Shell_common_usage.sh

          et_build_version=""
          install_scripts_env
          et_build_version=$(initial_et_build_version ${et_build_name_or_id})
          title="Bug Regression Reports For Build ${et_build_version}"

          cd RC_CI-master/auto_testing_CI
          echo "=== generating the bug content ==="
          python generate_confluence_page_for_jira.py ${confluence_username} ${confluence_password} ${et_fix_version}

          echo "=== Adding/updating pages to confluence page"
          content=$(cat content.txt)
          echo "=== update the confluence with the content ==="
          python confluence_client.py "${confluence_username}" "${confluence_password}" "${title}" "${space}" "${content}" "${jira_parent_page}"
          '''
        } //container
    } // node
  } //pod
} //call
