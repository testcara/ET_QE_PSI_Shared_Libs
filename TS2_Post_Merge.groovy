@Library('ET_TS2_Pipeline') _

def appName="cucumber-et-testing"
def templateNameofET = 'errata-rails-aio-template'
def templateNameofMysql = 'errata-mysql-template'
def templatePathofET = 'https://github.com/testcara/pipeline_file/raw/master/app_template.json'
def templatePathofMysql = 'https://github.com/testcara/pipeline_file/raw/master/mysql_template.json'
def etTemplateParameters = "-p=BREW_PVC=brew-qa -p=GIT_BRANCH=develop -p=CPU_LIMITS=1 -p=MEM_REQUEST=4Gi -p=MEM_LIMITS=4Gi -p=ET_CONFIGMAP=et-qe-testing-settings -p=ET_SECRET=et-qe-testing-settings -p=ET_MYSQL_SECRET=et-qe-testing-mysql"
def mysqlTemplateParameters = ""
def qeTesting = "true"
def api_username = 'wlin-admin'
def api_token = '112134cabf33644d1896a71db14df02b7c'
def credentials = '1d897d55-2951-4489-bf80-c8fe6903983c'
def casesGroup1 =  ' -n \'Older build and newer build of same package\' -n \'Product Security Approval\' -n \'Container Health Index Grade\' -n \'Login\' -n \'Calculate \\"reboot_suggested\\" value based on packages\' -n \'CDN Docker Push\' -n \'Add ability to bulk remove builds in an advisory\' -n \'Multi-Product\' -n \'A light weight empty module push e2e case\' -n \'The policy for role buildroot\' -n \'Edit advisory\' -n \'Batch\' -n \'Product Listing features\' -n \'RPM manifest in Container\' -t ~@disable' 
def casesGroup2 = " -n 'Attach multi-arch container builds to advisories' -n 'Brew' -n 'The rule of setting released build' -n 'RPM files should be shown on content page for RHEL-8 advisory' -n 'A simple example E2E test case' -n 'Errata and Bugzilla interactions' -n 'Add mixed types of builds in one same advisory' -n 'Bug advisory eligibility' -n 'Main stream release for RHEL product versions and variants' -n 'Push multiple advisories in a batch' -n 'Released build validation' -n 'bug eligibility checks for rhel8 bugs' -n 'Provide option to disable *' -t ~@disable" 
def casesGroup3 =  ' -n \'Add \\"Product Security Reviewer\\" field\' -n \'Embargoed Advisory dependency\' -n \'RPMDiff support for module builds\' -n \'CAT\' -n \'Product Listings\' -n \'Blacklist subpackages from RHN\' -n \'RHSAs require a CPE\' -n \'Module builds on builds tab\' -n \'Publish all erratum changes to UMB\' -n \'Add bug to advisory\' -t ~@disable'  
def casesDistribution = [casesGroup1, casesGroup2, casesGroup3]
def run_parallel = 'true'
def check_frequency = 'H/2 * * * *'

node {
    def token = sh returnStdout: true, script: 'oc whoami -t'
    properties([
        parameters([
             string(name: 'repo_url', defaultValue: 'https://code.engineering.redhat.com/gerrit/errata-rails', description: 'The target project repo we monitor'),
             string(name: 'mail_to', defaultValue: 'errata-qe-team@redhat.com', description: 'The receipts of the testing reprot')
             ]
            ),
        pipelineTriggers([pollSCM(check_frequency)]),
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '2', numToKeepStr: '')), 
        disableConcurrentBuilds()
        ])
    stage('checkout gerrit patchset') {
        // we track the develop and release branches.
        checkout([$class: 'GitSCM', branches: [[name: 'develop'],[name: 'release']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: credentials, url: params.repo_url]]])
        // if the current updated branch is release branch, let us override the default template parameter
        if (currentBuild.rawBuild.getLog(50).contains('(origin/release)')) {
           etTemplateParameters=etTemplateParameters + " -p=GIT_BRANCH=release"
        }
    }
    [templateNameofET, templateNameofMysql].each 
    {
        clean_up(token, it, 'template')
        
    }
    upload_templates(token, templatePathofET, templatePathofMysql)
    try {
        parallel(
            parallel_testing(token, appName, templateNameofET, templateNameofMysql, etTemplateParameters,
        mysqlTemplateParameters, templatePathofET, templatePathofMysql, qeTesting, casesDistribution, run_parallel)
            )
    } catch (Exception e){
        echo "Failed to run TS2.0 testing"
    } finally {
        send_hunter_report(api_username, api_token, params.mail_to)
    }

}

def parallel_testing(token, appName, templateNameofET, templateNameofMysql, etTemplateParameters,
    mysqlTemplateParameters, templatePathofET, templatePathofMysql, qeTesting, casesDistribution, run_parallel) {
    def buildStages = [:]

    casesDistribution.each {
        def index = casesDistribution.indexOf(it).toString()
        def groupName = "TS2 Parallel ${index}"
        def app = "${appName}-${index}"

        buildStages.put(
            groupName,

            pipeline_testing(groupName, token, app, templateNameofET, templateNameofMysql, etTemplateParameters,
        mysqlTemplateParameters, templatePathofET, templatePathofMysql, qeTesting, it, run_parallel)

            )
    } //each

    return buildStages            
        
}


def pipeline_testing(stageName, token, appName, templateNameofET, templateNameofMysql, etTemplateParameters,
    mysqlTemplateParameters, templatePathofET, templatePathofMysql, qeTesting, casesTags, run_parallel){

    return { 
        stage(stageName){
        ET_Pipeline(token, appName, templateNameofET, templateNameofMysql, etTemplateParameters,
    mysqlTemplateParameters, templatePathofET, templatePathofMysql, qeTesting, casesTags, run_parallel)
        }
    }
}