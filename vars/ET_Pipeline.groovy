def call(String token, String project_name, String bc_strategy, String appName, String templateNameofET,
    String etTemplateParameters, String templatePathofET, String qeTesting, String casesTags,
    String parallel, String current_branch, String remove_pods){

	  echo "---> Now, you are using the ET pipeline shared lib ..."

		// def RUN_USER = '1011210000'
		def MYSQL_USER = "errata"
		def MYSQL_PASSWORD = "errata"
		def runner= "mypod-${UUID.randomUUID().toString()}"
		// etTemplateParameters = etTemplateParameters + " -p=RUN_USER=$RUN_USER"
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
mountPath: '/mnt/redhat'),
persistentVolumeClaim(
claimName: 'errata-cert',
mountPath: '/tmp/errata-cert'),
persistentVolumeClaim(
claimName: 'brew-qa',
mountPath: '/mnt/brew'),
 ])
 {
	node(runner) {
        try {
        stage('create mysql app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              echo "app-name:${appName}-mariadb-102-rhel7"
              echo "${mysqlAppParameters}"
              openshift.withCluster('https://paas.psi.redhat.com', token) {
                 openshift.withProject(project_name){
                  sh "echo ${appName}-mariadb-102-rhel7 > appName"
                  sh "echo $mysqlAppParameters > appParameters"
                  sh "echo $mysqlImageRepo > repoImage"
                  sh "echo $project_name > projectName"
                  sh '''
                  appName=$(cat appName)
                  repoImage=$(cat repoImage)
                  appParameters=$(cat appParameters)
                  projectName=$(cat projectName)
                  echo oc new-app --name=${appName} -e ${appParameters} ${repoImage} -n ${projectName}
                  oc new-app --name=${appName} -e ${appParameters} ${repoImage} -n ${projectName}
                  '''
                } //project
              } //cluster
            } //retry
            retry(2){
              // We would wait 5 mins to make sure its deployed succesfully
              openshift.withCluster('https://paas.psi.redhat.com', token) {
                 openshift.withProject(project_name){
                    sh "echo ${appName}-mariadb-102-rhel7 > dcName"
                    sh "echo ${project_name} > projectName"
                    sh '''
                        projectName=$(cat projectName)
                        dcName=$(cat dcName)
                        # let us wait 5 mins
                        for i in {1..5}
                        do
                            sleep 60 # 10 seconds
                            status=$(oc get pods -n ${projectName} | grep ${dcName} | grep -v build | grep -v deploy | awk \'{print $3}\')
                            if [[ ${status} =~ "Running" ]]
                            then
                              echo "---> Deployment complete ..."
                              exit 0
                            elif [[ ${status} =~ "Failed" ]] || [[ ${status} =~ "Error" ]]
                            then
                              echo "---> Deployment has been failed ..."
                              exit 1
                            else
                              echo "---> Still running ..."
                              if [[ $i -eq 5 ]]
                              then
                                exit 1
                              fi
                            fi
                        done
                       '''
                } //project
              } // cluster
            } //retry
          } //container
        } //stage
        stage('create et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
                openshift.withCluster('https://paas.psi.redhat.com', token) {
                  openshift.withProject(project_name){
                    sh "echo $appName > name"
                    sh "echo $templateNameofET > templateParameters"
                    sh "echo $etTemplateParameters > template"
                    sh "echo $project_name > projectName"
                    sh '''
                    projectName=$(cat projectName)
                    app_name=$(cat name)
                    template=$(cat template)
                    templateParameters=$(cat templateParameters)
                    templateParameters="-p=APP_NAME=${app_name} ${templateParameters}"
                    oc process ${template} ${templateParameters} -n ${projectName} | oc create -f -  --namespace=${projectName} || true
                    '''
                  } //project
                } //cluster
            } //retry
          } //container
        } //stage
        stage('build et app'){
          container('qe-testing-runner'){
            script { FAILED_STAGE=env.STAGE_NAME }
            retry(2) {
              openshift.withCluster('https://paas.psi.redhat.com', token) {
                openshift.withProject(project_name){
                    sh "echo ${appName}-bc > bcName"
                    sh "echo ${project_name} > projectName"
                    sh "oc start-build ${appName}-bc -n ${projectName}"
                    sh '''
                    bcName=$(cat bcName)
                    # let us wait 30 mins
                    for i in {1..60}
                    do
                        sleep 30 # 30 seconds
                        status=$(oc get build -n ${projectName} | grep ${bcName} | awk \'{print $4}\')
                        if [[ ${status} =~ "Complete" ]]
                        then
                            echo "---> Build complete ..."
                            exit 0
                        elif [[ ${status} =~ "Failed" ]]
                        then
                            echo "---> Build has been failed ..."
                            exit 1
                        else
                            echo "---> Still running ..."
                        fi
                    done
                    '''
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
                openshift.withProject(project_name){
                    sh "echo ${project_name} > projectName"
                    echo "--- Deploy dc: ${appName}-rails--->"
                    sh "oc rollout latest ${appName}-rails  -n ${projectName}"
                    sh "echo ${appName}-rails > dcName"
                    sh '''
                    dcName=$(cat dcName)
                    projectName=$(cat projectName)
                    # let us wait 5 mins
                    for i in {1..30}
                    do
                        sleep 10 # 10 seconds
                        status=$(oc get pods -n ${projectName} | grep ${dcName} | grep -v build | grep -v deploy | awk \'{print $3}\')
                        if [[ ${status} =~ "Running" ]]
                        then
                            echo "---> Deployment complete ..."
                            exit 0
                        elif [[ ${status} =~ "Failed" ]] || [[ ${status} =~ "Error" ]]
                        then
                            echo "---> Deployment has been failed ..."
                            exit 1
                        else
                            echo "---> Still running ..."
                        fi
                    done
                    '''
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
                  openshift.withProject(project_name){
                    def cmd1="oc get pods -n " + project_name + " | grep ${appName}-mariadb-102-rhel7 | grep -v build | grep -v deploy | grep Running |cut -d ' ' -f 1"
                    def mysqlPod = sh(returnStdout: true, script: cmd1).trim()
                    echo "Got mysqlPod: ${mysqlPod}"

                    def cmd2="oc get pods -n " + project_name + " | grep ${appName}-rails | grep -v build | grep -v deploy | grep Running | cut -d ' ' -f 1"
                    def etPod = sh(returnStdout: true, script: cmd2).trim()
                    echo "Got etPod: ${etPod}"

                    import_sql_files_to_db(project_name, mysqlPod)
                    def db_migration_cmd = "bundle exec rake db:migrate"
                    run_cmd_against_pod(project_name, etPod, db_migration_cmd)

                    disable_sending_qpid_message(etPod)

                    if(casesTags.contains('UMB')){
                      specify_cucumber_umb_broker(etPod)
                    }

                    restart_et_service(etPod)

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
                openshift.withProject(project_name){
                sh "echo $current_branch > current_branch"
                sh "echo $project_name > projectName"
                sh '''
                current_branch=$(cat current_branch)
                projectName=$(cat projectName)
                git clone https://code.engineering.redhat.com/gerrit/errata-rails
                cd errata-rails
                git checkout ${current_branch}
                '''
                /*
                The following code does not work for the ssl error.

                git branch: 'develop',
                  url: 'https://code.engineering.redhat.com/gerrit/errata-rails'
                */

                def cmd="oc get pods -n " + project_name + " | grep ${appName}-rails | grep -v build | cut -d ' ' -f 1"
                def etPod = sh(returnStdout: true, script: cmd).trim()
                echo "Got etPod: ${etPod}"
                def etSVC=""
                if(bc_strategy == 'docker'){
                  cmd="oc get svc -n " + project_name + " | grep $appName | cut -d \" \" -f 1"
                  etSVC= sh(returnStdout: true, script: cmd).trim()
                } //if
                if(bc_strategy == 's2i'){
                  cmd="oc get routes -n " + project_name + " | grep $appName | sed  \"s/\\ \\+/ /g\" | cut -d \" \" -f 2"
                  etSVC= sh(returnStdout: true, script: cmd).trim()
                } //if
                run_ts2_testing(project_name, appName, etPod, casesTags, etSVC)
                } //project
              } //cluster
            } //container
          } //stage
        } //if
      }
      finally{
        archiveArtifacts '**/cucumber-report*.json'
        cucumber fileIncludePattern: "**/cucumber-report*.json", sortingMethod: "ALPHABETICAL"
        clean_ws()
        if(remove_pods=="true"){
          container('qe-testing-runner'){
            retry(2) {
              [appName, "${appName}-mariadb-102-rhel7"].each {
                if(parallel=="true"){
                  clean_up_by_oc(project_name, it, 'app')
                } else{
                  clean_up(project_name, it, 'app')
                } //if
              } //each
            } //retry
          } //container
        }//if_remove_pods
      } // finally
    } //node
  } //containerTemplate
}
