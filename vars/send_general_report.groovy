def call(String mail_to){
  script {
    def test_log = currentBuild.rawBuild.getLog(20)
    sh "echo $test_log > test_log"
    def build_version = sh returnStdout: true, script: '''
    test_log=$(cat test_log)
    echo $test_log | cut -d "=" -f 20- | cut -d "[" -f 1 | cut -d ',' -f 2 | cut -c 2-
    '''
    echo "et version: $build_version"

    def general_status = sh returnStdout: true, script: '''
    test_log=$(cat test_log)
    echo $test_log | cut -d "=" -f 20- | cut -d "[" -f 1 | cut -d ',' -f 3 | cut -c 2-
    '''
    echo "general_status: $general_status"

    def general_summary = sh returnStdout: true, script: '''
    test_log=$(cat test_log)
    echo $test_log | cut -d "=" -f 20- | cut -d "[" -f 1 | cut -d ',' -f 4- | cut -c 2- | tr -d ','
    '''
    echo "general_summary: $general_summary"


    def title = "ET Testing Reports For Build $build_version"
    echo "title: $title"

    def report_link = "https://docs.engineering.redhat.com/display/PDT/ET+Testing+Reports+For+Build+$build_version"
    echo "report_link: $report_link"

    echo "Sending mail now ..."
    String subject = "Testing Report for Build-" + build_version + "-" + general_status
    
    body = """
    <p style="font-family:arial">
    <p>ET Version: "$build_version"</p>
    <p>Testing Result: "$general_status"</p>
    <p>Testing Summary: "$general_summary"</p>
    <p>Testing Report: <a href="$report_link">"$title"</a></p>
    </p>
    """
    
    if (mail_to != null && !mail_to.isEmpty()) {
      // Email on any failures, and on first success.
      mail to: mail_to, subject: subject, body: body, mimeType: "text/html"
      echo 'Sent email notification'
    }
  }
}
