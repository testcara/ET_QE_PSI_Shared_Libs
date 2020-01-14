def call(String token, String bc_strategy, String appName, String templateNameofET, String templateNameofMysql,
    String etTemplateParameters, String mysqlTemplateParameters,
		String templatePathofET, String templatePathofMysql,
		String qeTesting, String casesTags, String parallel, String current_branch, String remove_pods){

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
        stage('create mysql app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              echo "app-name:${appName}-mariadb-102-rhel7"
              echo "${mysqlAppParameters}"
              openshift.withCluster('https://paas.psi.redhat.com', token) {
                 openshift.withProject('c3i-carawang-123'){
                  sh "echo ${appName}-mariadb-102-rhel7 > appName"
                  sh "echo $mysqlAppParameters > appParameters"
                  sh "echo $mysqlImageRepo > repoImage"
                  sh '''
                  app_name=$(cat appName)
                  repoImage=$(cat repoImage)
                  appParameters=$(cat appParameters)
                  echo oc new-app --name=${app_name} -e ${appParameters} ${repoImage}
                  oc new-app --name=${app_name} -e ${appParameters} ${repoImage}
                  '''
                } //project
              } //cluster
            } //retry
            retry(2){
              // We would wait 5 mins to make sure its deployed succesfully
              openshift.withCluster('https://paas.psi.redhat.com', token) {
                 openshift.withProject('c3i-carawang-123'){
                  track_deployment(token, "${appName}-mariadb-102-rhel7")
                } //project
              } // cluster
            } //retry
          } //container
        } //stage
        stage('create et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              if(parallel=="true"){
                openshift.withCluster('https://paas.psi.redhat.com', token) {
                  openshift.withProject('c3i-carawang-123'){
                    create_apps_by_template_with_oc(token, appName, templateNameofET, etTemplateParameters)
                  } //project
                } //cluster
              } else{
                openshift.withCluster('https://paas.psi.redhat.com', token) {
                  openshift.withProject('c3i-carawang-123'){
                    create_apps_by_template_without_oc(token, appName, templateNameofET, etTemplateParameters)
                  }//project
                } //cluseter
              } //else
            } //retry
          } //container
        } //stage
        stage('build et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              openshift.withCluster('https://paas.psi.redhat.com', token) {
                openshift.withProject('c3i-carawang-123'){
                  build_bc_and_track_build_by_oc(token, 20, "${appName}-bc")
                } //project
              } //cluster
            } //retry
          } //container
        } //stage
        stage('deploy et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              openshift.withCluster('https://paas.psi.redhat.com', token) {
                openshift.withProject('c3i-carawang-123'){
                  deploy_dc_and_track_deployment_by_oc(token, 10, "${appName}-rails")
                } //project
              } //cluster
            } //retry
          } //container
        } //stage
        if(qeTesting=='true'){
          stage('TS2 testing preparation'){
              container('qe-testing-runner'){
                script { FAILED_STAGE=env.STAGE_NAME }
                retry(2) {
                  openshift.withCluster('https://paas.psi.redhat.com', token) {
                  openshift.withProject('c3i-carawang-123'){
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
                    } //project
                  } //cluster
                } //retry
             } //container
          } //stage

          stage('run TS2 testing'){
            container('qe-testing-runner'){
              script { FAILED_STAGE=env.STAGE_NAME }
              openshift.withCluster('https://paas.psi.redhat.com', token) {
                openshift.withProject('c3i-carawang-123'){
                sh "echo $current_branch > current_branch"
                sh '''
                current_branch=$(cat current_branch)
                git clone https://code.engineering.redhat.com/gerrit/errata-rails
                cd errata-rails
                git checkout ${current_branch}
                '''
                /*
                The following code does not work for the ssl error.

                git branch: 'develop',
                  url: 'https://code.engineering.redhat.com/gerrit/errata-rails'
                */

                def cmd="oc get pods | grep ${appName}-rails | grep -v build | cut -d ' ' -f 1"
                def etPod = sh(returnStdout: true, script: cmd).trim()
                echo "Got etPod: ${etPod}"
                def etSVC=""
                if(bc_strategy == 'docker'){
                  cmd="oc get svc | grep $appName | cut -d \" \" -f 1"
                  etSVC= sh(returnStdout: true, script: cmd).trim()
                } //if
                if(bc_strategy == 's2i'){
                  cmd="oc get routes | grep $appName | sed  \"s/\\ \\+/ /g\" | cut -d \" \" -f 2"
                  etSVC= sh(returnStdout: true, script: cmd).trim()
                } //if
                run_ts2_testing(token, appName, etPod, casesTags, etSVC)
                } //project
              } //cluster
            } //container
          } //stage
        } //if
    } //node
  } //containerTemplate
}
