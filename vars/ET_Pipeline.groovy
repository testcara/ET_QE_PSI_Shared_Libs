def call(String token, String appName, String templateNameofET, String templateNameofMysql,
  String etTemplateParameters, String mysqlTemplateParameters,
  String templatePathofET, String templatePathofMysql,
  String qeTesting, String casesTags, String parallel){

  echo "---> Now, you are using the ET pipeline shared lib ..."

  def RUN_USER = '1058980001'
  def MSYQL_USER = "root"
  def MYSQL_PASSWORD = "arNdk123_"
  def DB_FILE = "/tmp/TS2_db/errata.latest.sql"
  def runner= "mypod-${UUID.randomUUID().toString()}"
  etTemplateParameters = etTemplateParameters + " -p=RUN_USER=$RUN_USER"
  def FAILED_STAGE

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
    claimName: 'et-qe-testing-mysql',
    mountPath: '/tmp/TS2_db/'),
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
              [appName, "${appName}-mysql"].each {
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
                [templateNameofET, templateNameofMysql].each {
                  clean_up(token, it, 'template')
                } //each
                upload_templates(token, templatePathofET, templatePathofMysql)
              } //retry
            } //container
          } //stage
        } //if

        stage('create mysql app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              echo "app-name:${appName}-mysql "
              if(parallel=="true"){
                create_apps_by_oc(token, "${appName}-mysql", templateNameofMysql, mysqlTemplateParameters)
              } else{
                create_apps(token, "${appName}-mysql", templateNameofMysql, mysqlTemplateParameters)
              } //if
            } //retry
          } //container
        } //stage
        stage('create et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              if(parallel=="true"){
                create_apps_by_oc(token, appName, templateNameofET, etTemplateParameters)
              } else{
                create_apps(token, appName, templateNameofET, etTemplateParameters)
              }
            } //retry
          } //container
        } //stage
        stage('build mysql app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              build_bc_and_track_build_by_oc(token, 20, "${appName}-mysql")
            } //retry
          } //container
        } //stage
        stage('deploy mysql app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              deploy_dc_and_track_deployment_by_oc(token, 5, "${appName}-mysql")
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
              deploy_dc_and_track_deployment_by_oc(token, 5, "${appName}-rails")
            } //retry
          } //container
        } //stage
        if(qeTesting=='true'){
          stage('TS2 testing preparation'){
              container('qe-testing-runner'){
                script { FAILED_STAGE=env.STAGE_NAME }
                retry(2) {
                    def cmd1="oc get pods | grep ${appName}-mysql | grep -v build | grep -v deploy |cut -d ' ' -f 1"
                    def mysqlPod = sh(returnStdout: true, script: cmd1).trim()
                    echo "Got mysqlPod: ${mysqlPod}"

                    def cmd2="oc get pods | grep ${appName}-rails | grep -v build | grep -v deploy | cut -d ' ' -f 1"
                    def etPod = sh(returnStdout: true, script: cmd2).trim()
                    echo "Got etPod: ${etPod}"

                    import_sql_files_to_db(token, mysqlPod, DB_FILE, MSYQL_USER, MYSQL_PASSWORD)
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
              sh '''
              git clone https://code.engineering.redhat.com/gerrit/errata-rails
              cd errata-rails
              git checkout develop
              '''
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
        sh "echo Exception for testing ${appName}: Failed at ${FAILED_STAGE} > ${appName}_failed_stages"
      } //catch
      finally{
        sleep(3600)
        archiveArtifacts '*_failed_stages'
        archiveArtifacts '**/cucumber-report*.json'
        cucumber fileIncludePattern: "**/cucumber-report*.json", sortingMethod: "ALPHABETICAL"
        clean_ws()
      } // finally
    } //node
  } //container
}
