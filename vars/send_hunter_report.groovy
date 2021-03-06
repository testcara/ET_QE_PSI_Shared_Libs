def call(String api_username, String api_token, String mail_to, String testing_type = '', String branch = '') {
    String to = mail_to
    String currentResult = ''
    String latestCommit = ''
    if (branch != '') {
       def workspace = pwd()
       echo "$workspace"
       sh "echo $branch > current_branch"
       latestCommit = sh returnStdout: true, script: '''
       current_branch=$(cat current_branch)
       if [[ -d 'errata-rails' ]]
       then
         cd errata-rails
       else
         git clone https://code.engineering.redhat.com/gerrit/errata-rails >> /dev/null
         cd errata-rails
       fi
       # Without >> /dev/null, the output of this step makes the latestCommit contain some unexpected strings
       git checkout $current_branch -f >> /dev/null
       git pull || true >> /dev/null
       commit=$(git rev-parse HEAD| xargs -n1 | tail -n1)
       echo $commit
       '''
       echo "$latestCommit"
    } else {
      latestCommit = sh(returnStdout: true, script: 'git rev-parse HEAD')
    }

    String latestCommitShort = sh(returnStdout: true, script: 'git rev-parse HEAD | cut -c 1-10')
    def causes = currentBuild.rawBuild.getCauses()
    // E.g. 'started by user', 'triggered by scm change'
    def cause = null
    if (!causes.isEmpty()) {
        cause = causes[0].getShortDescription()
    }

    causes = null

    String cucumber_report_url = env.BUILD_URL + "/cucumber-html-reports/overview-features.html"
    String cucumber_failure_url = env.BUILD_URL + "cucumber-html-reports/overview-failures.html"
    String test_report_url = ""

    String body = """
    <p style='font-family:arial'>
    <p>Latest Commit: "$latestCommit"</p>
    <p>Build Trigger: $cause</p>
    <p>For more details, please reach the <a href="$env.BUILD_URL">build log</a> or the original <a href="$cucumber_report_url">cucumber report.</a></p>
    </p>
    """

    sh "echo $testing_type > testing_type"

    sh "curl --insecure -X GET -u $api_username:$api_token $cucumber_failure_url >  cucumber_failure_report.html"
    sh "curl --insecure -X GET -u $api_username:$api_token $cucumber_report_url >  cucumber_report.html"

    sh "grep -b12 \'<tfoot\' cucumber_report.html | tail -n1 | cut -d \'>\' -f 2 | cut -d \'<\' -f 1 > total_scenarios_num"

    String general_report = sh returnStdout: true, script: '''
    # Sometimes, there is no cucumber report, curl will generate 'Not found' report
    deal_empty_report(){
        if [[ ${total_scenarios_num} == "" ]]
        then
          echo "<p>Error: The cucumber report is not available. Please search 'Failed at' to locate failed stage in the original build log!</p>"
          exit 0
        fi
    }
    get_blame_file(){
        # failed_scenarios or disable_scenarios are needed as the parameter
        rm -rf blame_file
        cat $1 | while read line
        do
        grep -r "$line" features/remote/scenarios/ | head -n1 | awk -F\':\' \'{print $1}\' | xargs -i git blame "{}"  >> blame_file
        done
    }
    get_owner_and_scenarios_map(){
        # failed_scenarios or disable_scenarios are needed as the parameter
        cat $1 | while read line; do grep "$line" blame_file|head -n1 ; done  > blame_owners
        cat blame_owners | cut -d "(" -f 2   | cut -d " " -f 1 > owner
        cat owner | sed -e "i<tr><td>" | sed -n "{N;s/\\n//p}" > format_owner
        cat $1  | sed -e "i</td><td>" | sed -n "{N;s/\\n//p}" | sed -e "a</td></tr>" | sed -n "{N;s/\\n//p}" > format_scenarios
        paste format_owner format_scenarios > owner_scenarios
    }

    total_scenarios_num=$(cat total_scenarios_num)
    deal_empty_report

    # Get the failure report
    cat cucumber_failure_report.html | grep -b1 Scenario  | grep -v "brief" | awk \'{$1=""; print $0}\' | cut -d ">" -f 2- | cut -d "<" -f 1 | sed "/^$/d"| sed -n "{N;s/\\n/: /p}" | sort -u > failed_scenarios
    get_blame_file failed_scenarios
    get_owner_and_scenarios_map failed_scenarios
    questionable_cases_num=$(cat owner_scenarios | wc -l)

    if [[ ${questionable_cases_num} -eq 0 ]]
    then
    echo "<p style=\'font-family:arial; line-height:0px;\'>No failures, cheers!</p>" >> owner_scenarios
    fi

    failure_percentage=$(awk "BEGIN {print (${questionable_cases_num}/$total_scenarios_num*100)}" | cut -c 1-5)

    sed -i "1 i <p style=\'font-family:arial; font-size: 1em; font-weight: bold\'>Failed Scenarios(${questionable_cases_num}/$total_scenarios_num=${failure_percentage}%)</p>" owner_scenarios
    failed_scenarios_report=$(cat owner_scenarios)

    # Get the pending report
    grep -r -b1 "@disable" features/remote/scenarios/ | grep "Scenario" | awk \'{$1=""; print $0}\' |  sed -e "s/^[ \\t]*//"  | sort -u > disable_scenarios
    questionable_cases_num=$(cat disable_scenarios | wc -l)
    get_blame_file disable_scenarios
    get_owner_and_scenarios_map disable_scenarios

    questionable_cases_num=$(cat owner_scenarios | wc -l)
    if [[ ${questionable_cases_num} -eq 0 ]]
    then
    echo "<p style=\'font-family:arial\'>No pending scenarios, cheers!</p>" >> owner_scenarios
    fi

    disable_percentage=$(awk "BEGIN {print (${questionable_cases_num}/${total_scenarios_num}*100)}" | cut -c 1-5)
    sed -i "1 i <p style=\'font-family:arial; font-size: 1em; font-weight: bold\'>Pending Scenarios(${questionable_cases_num}/${total_scenarios_num}=${disable_percentage}%)</p>" owner_scenarios
    disable_scenarios_report=$(cat owner_scenarios)
    report=""

    testing_type=$(cat testing_type)
    if [[ "${testing_type}" =~ "E2E" ]]
    then
      report="<pre>${failed_scenarios_report}</pre>"
    else
      report="<pre>${failed_scenarios_report}${disable_scenarios_report}</pre>"
    fi
    echo ${report}
    '''

    if ("$general_report" =~ "No failures, cheers!"){
        currentResult = 'SUCCESS'
        test_report_url = cucumber_report_url
    } else if ("$general_report" =~ "The cucumber report is not available"){
        currentResult = 'FAILED'
        test_report_url = env.BUILD_URL
    } else {
        currentResult = 'FAILED'
        test_report_url = cucumber_report_url
    }

    body = body + general_report
    echo "Body ..."
    echo body

    sh "echo \"$latestCommitShort($branch)\" > commit"
    sh "echo $testing_type > testing_type"
    sh "echo $currentResult > current_result"
    sh "echo $test_report_url > test_report_url"
    sh '''
    export commit=$(cat commit | sed "s/Already up-to-date.//g" | xargs)
    export testing_type=$(cat testing_type)
    export current_result=$(cat current_result)
    export test_report_url=$(cat test_report_url)

    echo "=====================Testing Report: Begin=================="
    echo "ET Version/Commit: ${commit}"
    echo "Testing Type: ${testing_type}"
    echo "Testing Result: ${current_result}"
    echo "Testing Report URL: ${test_report_url}"
    echo "=====================Testing Report: End================"
    '''

    echo "Sending mail now ..."
    String subject = "$currentResult: $env.JOB_NAME#$env.BUILD_NUMBER for Commit $latestCommitShort"
    if (to != null && !to.isEmpty()) {
        // Email on any failures, and on first success.
        mail to: to, subject: subject, body: body, mimeType: "text/html"
        echo 'Sent email notification'
    }
}
