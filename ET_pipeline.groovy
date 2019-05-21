@Library('ET_TS2_Pipeline') _
def token = "XejtLJzRPzrBS5tiMrtqXv8UajH0zVgWCpq-F5XeUBo"
def app_name="cucumber-et"
def templateNameofET = 'errata-rails-aio-template'
def templateNameofMysql = 'errata-mysql-template'
def templatePathofET = 'https://github.com/testcara/pipeline_file/raw/master/app_template.json'
def templatePathofMysql = 'https://github.com/testcara/pipeline_file/raw/master/mysql_template.json'
def RUN_USER = '1058980001'
def etTemplateParameters = "-p=APP_NAME=${app_name} -p=CPU_LIMITS=1 -p=MEM_REQUEST=4Gi -p=MEM_LIMITS=4Gi -p=ET_CONFIGMAP=et-qe-testing-settings -p=ET_SECRET=et-qe-testing-settings -p=ET_MYSQL_SECRET=et-qe-testing-mysql -p=RUN_USER=$RUN_USER"
def mysqlTemplateParameters = "-p=APP_NAME=${app_name}-mysql"
def runner_1 = "mypod-${UUID.randomUUID().toString()}"
def QE_TESTING = "true"
def MSYQL_USER = "root"
def MYSQL_PASSWORD = "arNdk123_"
def DB_FILE = "/tmp/TS2_db/errata.latest.sql"
def cases_tags = "--tags @umb"

podTemplate(label: runner_1, 
    containers: [
    containerTemplate(
        name: 'qe-testing-runner', 
        image: 'docker-registry.upshift.redhat.com/errata-qe-test/qe_testing_upshift_runner:latest',
        command: 'cat', 
        ttyEnabled: true,
        
        envVars: [
            envVar(key: 'GIT_SSL_NO_VERIFY', value: 'true')
        ]
        
        )],
    volumes: [
    persistentVolumeClaim(
        claimName: 'et-qe-testing-mysql',
        mountPath: '/tmp/TS2_db/')
    ]) 
{ 
    node(runner_1) {   
        try {         
            stage('run TS2 testing'){
                container('qe-testing-runner'){
                    sh '''
                    printenv | grep GIT_S
                    git clone https://code.engineering.redhat.com/gerrit/errata-rails
                    cd errata-rails
                    git checkout develop
                    '''
                    /*
                    The following code does not work for the ssl error.
                    
                    git branch: 'develop',
                        url: 'https://code.engineering.redhat.com/gerrit/errata-rails'
                    */

                    def etPod = get_pod_name_for_dc(token, "${app_name}-rails")
                    run_ts2_testing(token, app_name, etPod, cases_tags)
                } //container
            } //stage
        } //try
        catch(Exception e) {
            echo "Failed to do QE testing ..."
        }
        finally{
            archiveArtifacts '**/cucumber-report*.json'
            cucumber fileIncludePattern: "**/cucumber-report*.json", sortingMethod: "ALPHABETICAL"
        }
    } //node
  } //container
