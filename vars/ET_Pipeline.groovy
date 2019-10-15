def call(String token, String appName, String templateNameofET, String templateNameofMysql,
  String etTemplateParameters, String mysqlTemplateParameters,
  String templatePathofET, String templatePathofMysql,
  String qeTesting, String casesTags, String parallel, String pub_jenkins_build){

  echo "---> Now, you are using the ET pipeline shared lib ..."

  def RUN_USER = '1058980001'
  def MYSQL_USER = "errata"
  def MYSQL_PASSWORD = "errata"
  def runner= "mypod-${UUID.randomUUID().toString()}"
  etTemplateParameters = etTemplateParameters + " -p=RUN_USER=$RUN_USER"
  def FAILED_STAGE
  def MYSQL_DATABASE = 'errata'
  def mysqlAppParameters="MYSQL_USER=" + MYSQL_USER + " -e MYSQL_ROOT_PASSWORD="+ MYSQL_PASSWORD + " -e MYSQL_PASSWORD=" + MYSQL_PASSWORD + " -e MYSQL_DATABASE=" + MYSQL_DATABASE
  def mysqlImageRepo="registry.access.redhat.com/rhscl/mariadb-102-rhel7"


  podTemplate(label: runner,
  containers: [
  containerTemplate(
    name: 'qe-testing-runner',
    image: 'docker-registry.upshift.redhat.com/errata-qe-test/qe_testing_upshift_runner:latest',
    alwaysPullImage: true,
    command: 'cat',
    ttyEnabled: true,
    envVars: [
      envVar(key: 'GIT_SSL_NO_VERIFY', value: 'true')
    ]

    )],
  volumes: [
  persistentVolumeClaim(
    claimName: 'pvc-errata-qe-test-mnt-redhat',
    mountPath: '/mnt/redhat')
  ])
  {
    node(runner) {
      try {
        stage('clean apps') {
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              [appName, "${appName}-mariadb-102-rhel7"].each {
                if(parallel=="true"){
                  clean_up_by_oc(token, it, 'app')
                } else{
                  clean_up(token, it, 'app')
                } //if
              } //each
            } //retry
          } //container
        } //stage
        if(parallel=="false"){
          stage('prepare templates'){
            container('qe-testing-runner'){
              script { FAILED_STAGE=env.STAGE_NAME }
              retry(2) {
                clean_up(token, templateNameofET, 'template')
                upload_templates(token, templatePathofET)
              } //retry
            } //container
          } //stage
        } //if

        stage('create mysql app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              echo "app-name:${appName}-mariadb-102-rhel7"
              echo "${mysqlAppParameters}"
              create_apps_by_new_app_with_oc(token, "${appName}-mariadb-102-rhel7", mysqlAppParameters, mysqlImageRepo)
            } //retry
            retry(2){
              // We would wait 5 mins to make sure its deployed succesfully
              track_deployment(token, "${appName}-mariadb-102-rhel7")
            }
          } //container
        } //stage
        stage('create et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              if(parallel=="true"){
                create_apps_by_template_with_oc(token, appName, templateNameofET, etTemplateParameters)
              } else{
                create_apps_by_template_without_oc(token, appName, templateNameofET, etTemplateParameters)
              }
            } //retry
          } //container
        } //stage
        stage('build et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              build_bc_and_track_build_by_oc(token, 20, "${appName}-bc")
            } //retry
          } //container
        } //stage
        stage('deploy et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              deploy_dc_and_track_deployment_by_oc(token, 10, "${appName}-rails")
            } //retry
          } //container
        } //stage
        if(qeTesting=='true'){
          stage('TS2 testing preparation'){
              container('qe-testing-runner'){
                script { FAILED_STAGE=env.STAGE_NAME }
                retry(2) {
                    def cmd1="oc get pods | grep ${appName}-mariadb-102-rhel7 | grep -v build | grep -v deploy | grep Running |cut -d ' ' -f 1"
                    def mysqlPod = sh(returnStdout: true, script: cmd1).trim()
                    echo "Got mysqlPod: ${mysqlPod}"

                    def cmd2="oc get pods | grep ${appName}-rails | grep -v build | grep -v deploy | grep Running | cut -d ' ' -f 1"
                    def etPod = sh(returnStdout: true, script: cmd2).trim()
                    echo "Got etPod: ${etPod}"

                    import_sql_files_to_db(token, mysqlPod)
                    def db_migration_cmd = "bundle exec rake db:migrate"
                    run_cmd_against_pod(token, etPod, db_migration_cmd)

                    disable_sending_qpid_message(token, etPod)

                    if(casesTags.contains('UMB')){
                      specify_cucumber_umb_broker(token, etPod)
                    }

                    restart_et_service(token, etPod)

                    echo "Add the pulp configuration files to runner"
                    sh """
                    if [[ ! -d "~/.rcm" ]]; then
                      mkdir ~/.rcm
                    fi
                    cp /tmp/pulp_configs/.rcm/pulp-environments.json ~/.rcm/
                    """
                } //retry
             } //container
          } //stage

          stage('run TS2 testing'){
            container('qe-testing-runner'){
              script { FAILED_STAGE=env.STAGE_NAME }
              if(pub_jenkins_build!='none' && pub_jenkins_build!=''){
                sh '''
                git clone https://code.engineering.redhat.com/gerrit/errata-rails
                cd errata-rails
                git checkout release
                '''
              }else{
                sh '''
                git clone https://code.engineering.redhat.com/gerrit/errata-rails
                cd errata-rails
                git checkout develop
                '''
              }
              /*
              The following code does not work for the ssl error.

              git branch: 'develop',
                url: 'https://code.engineering.redhat.com/gerrit/errata-rails'
              */

              def cmd="oc get pods | grep ${appName}-rails | grep -v build | cut -d ' ' -f 1"
              def etPod = sh(returnStdout: true, script: cmd).trim()
              echo "Got etPod: ${etPod}"
              run_ts2_testing(token, appName, etPod, casesTags)
            } //container
          } //stage
        } //if
      } //try
      catch(Exception e) {
        echo "Exception for testing ${appName}: Failed at ${FAILED_STAGE}"
      } //catch
      finally{
        archiveArtifacts '**/cucumber-report*.json'
        cucumber fileIncludePattern: "**/cucumber-report*.json", sortingMethod: "ALPHABETICAL"
        clean_ws()
        container('qe-testing-runner'){
          retry(2) {
            [appName, "${appName}-mariadb-102-rhel7"].each {
              if(parallel=="true"){
                clean_up_by_oc(token, it, 'app')
              } else{
                clean_up(token, it, 'app')
              } //if
            } //each
          } //retry
        } //container
      } // finally
    } //node
  } //containerTemplate
}
