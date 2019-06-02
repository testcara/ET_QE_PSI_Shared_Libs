def call(String api_username, String api_token) {
    def to = emailextrecipients([[$class: 'CulpritsRecipientProvider'],
                                 [$class: 'DevelopersRecipientProvider'],
                                 [$class: 'RequesterRecipientProvider']])
    String currentResult = ""
    String previousResult = currentBuild.getPreviousBuild().result
    String latestCommit = sh(returnStdout: true, script: 'git rev-parse HEAD')
    String latestCommitShort = sh(returnStdout: true, script: 'git rev-parse HEAD | cut -c 1-10')

    def causes = currentBuild.rawBuild.getCauses()
    // E.g. 'started by user', 'triggered by scm change'
    def cause = null
    if (!causes.isEmpty()) {
        cause = causes[0].getShortDescription()
    }

    causes = null


    String cucumber_report_url = "https://jenkins-errata-qe-test.cloud.paas.psi.redhat.com/job/test_container_template/396/cucumber-html-reports/overview-features.html"
    String cucumber_failure_url = "https://jenkins-errata-qe-test.cloud.paas.psi.redhat.com/job/test_container_template/396/cucumber-html-reports/overview-failures.html"
    //String cucumber_report_url = $env.BUILD_URL + "/cucumber-html-reports/overview-features.html"
    String body = """
    <p style='font-family:arial'>
    <p>Latest Commit: "$latestCommit"</p>
    <p>Build Trigger: $cause</p>
    <p>For more details, please reach the <a href="$env.BUILD_URL">build Log</a> and the original <a href="$cucumber_report_url">cucumber report.</a></p>
    </p>
    """

    sh "curl --insecure -X GET -u $api_username:$api_token $cucumber_failure_url >  cucumber_failure_report.html"
    sh "curl --insecure -X GET -u $api_username:$api_token $cucumber_report_url >  cucumber_report.html"
    
    sh "grep -b12 \'<tfoot\' cucumber_report.html | tail -n1 | cut -d \'>\' -f 2 | cut -d \'<\' -f 1 > total_scenarios_num"

    String failed_scenarios_report = sh returnStdout: true, script: '''
    total_scenarios_num=$(cat total_scenarios_num)
    cat cucumber_failure_report.html | grep -b1 Scenario  | grep -v "brief" | awk \'{$1=""; print $0}\' | cut -d ">" -f 2- | cut -d "<" -f 1 | sed "/^$/d"| sed -n "{N;s/\\n/: /p}" | sort -u > failed_scenarios
    rm -rf blame_file
    cat failed_scenarios | while read line
    do
    grep -r "$line" features/remote/scenarios/ | head -n1 | awk -F\':\' \'{print $1}\' | xargs -i git blame "{}"  >> blame_file
    done
    cat failed_scenarios | while read line; do grep "$line" blame_file|head -n1 ; done  > blame_owners
    cat blame_owners | cut -d "(" -f 2   | cut -d " " -f 1 > owner
    cat owner | sed -e "i<tr><td>" | sed -n "{N;s/\\n//p}" > format_owner
    cat failed_scenarios  | sed -e "i</td><td>" | sed -n "{N;s/\\n//p}" | sed -e "a</td></tr>" | sed -n "{N;s/\\n//p}" > format_scenarios

    paste format_owner format_scenarios > owner_scenarios
    questionable_cases_num=$(cat owner_scenarios | wc -l)

    if [[ ${questionable_cases_num} -eq 0 ]]
    then
    echo "None"
    else
    failure_percentage=$(awk "BEGIN {print (${questionable_cases_num}/$total_scenarios_num*100)}" | cut -c 1-5)
    sed -i "1 i <h3>Failed Scenarios(${questionable_cases_num}/$total_scenarios_num=${failure_percentage}%):</h3>" owner_scenarios
    cat owner_scenarios
    fi 
    '''
    String pending_scenarios_report = sh returnStdout: true, script: '''
    total_scenarios_num=$(cat total_scenarios_num)
    grep -r -b1 "@disable" features/remote/scenarios/ | grep "Scenario" | awk \'{$1=""; print $0}\' |  sed -e "s/^[ \\t]*//"  | sort -u > disable_scenarios
    questionable_cases_num=$(cat disable_scenarios | wc -l)
    cat disable_scenarios | while read line
    do
    grep -r "$line" features/remote/scenarios/ | head -n1 | awk -F\':\' \'{print $1}\' | xargs -i git blame "{}"  >> blame_file  
    done
    cat disable_scenarios | while read line; do grep "$line" blame_file|head -n1 ; done  > blame_owners
    cat blame_owners | cut -d "(" -f 2   | cut -d " " -f 1 > owner
    cat owner | sed -e "i<tr><td>" | sed -n "{N;s/\\n//p}" > format_owner
    cat disable_scenarios  | sed -e "i</td><td>" | sed -n "{N;s/\\n//p}" | sed -e "a</td></tr>" | sed -n "{N;s/\\n//p}" > format_scenarios
    
    paste format_owner format_scenarios > owner_scenarios
    questionable_cases_num=$(cat owner_scenarios | wc -l)
    if [[ ${questionable_cases_num} -eq 0 ]]
    then
    echo "None"
    else
    disable_percentage=$(awk "BEGIN {print (${questionable_cases_num}/${total_scenarios_num}*100)}" | cut -c 1-5)
    sed -i "1 i <h3>Disabled/Pending Scenarios(${questionable_cases_num}/${total_scenarios_num}=${disable_percentage}%):</h3>" owner_scenarios
    cat owner_scenarios
    fi 
    ''' 

    if ("$failed_scenarios_report" == "None"){
        currentResult = 'SUCCESS'
    }
    else {
        currentResult = 'FAILED'
    }



    body = body + """
<pre>
$failed_scenarios_report
</pre>
<pre>
$pending_scenarios_report
</pre>
"""

    String subject = " $currentResult: $env.JOB_NAME#$env.BUILD_NUMBER for Commit $latestCommitShort"
    if (to != null && !to.isEmpty()) {
        // Email on any failures, and on first success.
        mail to: to, subject: subject, body: body, mimeType: "text/html"
        echo 'Sent email notification'
    }
}
