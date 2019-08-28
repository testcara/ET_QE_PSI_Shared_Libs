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
        sh "echo $APiToken > APiToken"
        sh "echo $TS2PostMergePipeline > TS2PostMergePipeline"
        sh "echo $JENKINS_URL > JENKINS_URL"
        result = sh returnStdout: true, script: '''
          whoami || true >> /dev/dull
          psi-jenkins-slave
          whoami >>/dev/dull


          source /etc/bashrc >> /dev/dull
          export current_cucumber_report=$(cat current_cucumber_report)
          export APIUsername=$(cat APIUsername)
          export APIToken=$(cat APIToken)
          export TS2PostMergePipeline=$(cat TS2PostMergePipeline)
          export JENKINS_URL=$(cat JENKINS_URL)
          echo "===============Download the CI files under $(pwd)==========" >> /dev/null
          wget http://github.com/testcara/RC_CI/archive/master.zip
          unzip master.zip
          source RC_CI-master/auto_testing_CI/CI_Shell_prepare_env_and_scripts.sh
          source RC_CI-master/auto_testing_CI/CI_Shell_common_usage.sh

          install_scripts_env
          echo "Compare the reports and return the results" >> /dev/null
          python RC_CI-master/auto_testing_CI/compare_cucumber_reports.py "${APIUsername}" "${APIToken}" "${JENKINS_URL}" \
           "${TS2PostMergePipeline}" "${current_cucumber_report}"
          '''
        String subject = "Feature Monitoring Report"
        String body = result
        body = body + "$BUILD_URL"

        if ("$result" =~ "Attention"){
          subject = subject + ": ATTENTION!"
          if (to != null && !to.isEmpty()) {
            // Email on any failures, and on first success.
            mail to: to, subject: subject, body: body, mimeType: "text/html"
            echo 'Sent email notification'
          }

        }
      } //container
    } // node
  } //pod
} //call
