def call(String APIUsername, String APIToken, String TS2PostMergePipeline, String MailTo){
  def runner = "mypod-${UUID.randomUUID().toString()}"
  String to = MailTo
  String current_cucumber_report = ${BUILD_URL} + "cucumber-html-reports/overview-features.html"

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
        sh "echo $current_cucumber_report > current_cucumber_report"
        sh "echo $APIUsername > APIUsername"
        sh "echo $APIToken > APIToken"
        sh "echo $TS2PostMergePipeline > TS2PostMergePipeline"
        sh "echo $JENKINS_URL > JENKINS_URL"
        result = sh returnStdout: true, script: '''
          whoami || true >> useless_log
          psi-jenkins-slave >> useless_log
          whoami >> useless_log


          source /etc/bashrc >> useless_log
          export current_cucumber_report=$(cat current_cucumber_report)
          export APIUsername=$(cat APIUsername)
          export APIToken=$(cat APIToken)
          export TS2PostMergePipeline=$(cat TS2PostMergePipeline)
          export JENKINS_URL=$(cat JENKINS_URL)
          echo "===============Download the CI files under $(pwd)==========" >> useless_log
          wget http://github.com/testcara/RC_CI/archive/master.zip >> useless_log
          unzip master.zip >> useless_log
          source RC_CI-master/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh >> useless_log
          source RC_CI-master/auto_testing_CI/CI_Shell_common_usage.sh >> useless_log

          install_scripts_env >> useless_log
          echo "Compare the reports and return the results" >> useless_log
          export RC_Jenkins_URL=$JENKINS_URL
          python RC_CI-master/auto_testing_CI/compare_cucumber_reports.py "${APIUsername}" "${APIToken}"  \
           "${TS2PostMergePipeline}" "${current_cucumber_report}"
          '''
        String subject = "Feature Monitoring Report"
        String body = "<body><b>" + result + "</b>"
        body = body + "<p>You can get more details by: $BUILD_URL</p></body>"

        if ("$result" =~ "Attention"){
          subject = subject + ": Attention!"
        } else {
          subject = subject + ": Success!"
        } //if
        if (to!= null && !to.isEmpty()) {
          // Email on any failures, and on first success.
          mail to: to, subject: subject, body: body, mimeType: "text/html"
          echo 'SUCCESS: Sent email notification!'
        } //if
      } //container
    } // node
  } //pod
} //call
