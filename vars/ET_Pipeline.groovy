def call(String token, String appName, String templateNameofET, String templateNameofMysql, 
	String etTemplateParameters, String mysqlTemplateParameters,
	String templatePathofET, String templatePathofMysql,
	String qeTesting, String casesTags){

	def RUN_USER = '1058980001'
	def MSYQL_USER = "root"
	def MYSQL_PASSWORD = "arNdk123_"
	def DB_FILE = "/tmp/TS2_db/errata.latest.sql"
	def runner= "mypod-${UUID.randomUUID().toString()}"
	podTemplate(label: runner, 
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
	    node(runner) {
	        stage('clean apps') {
	            container('qe-testing-runner'){
	                [appName, "${appName}-mysql"].each {
	                    clean_up(token, it, 'app')
	                } //each
	                /* not clean up template here
	                [templateNameofET, templateNameofMysql].each {
	                    clean_up(token, it, 'template')
	                }
	                */
	            } //container
	        } //stage
	        
	        /*
	        stage('upload templates') {
	            container('qe-testing-runner'){
	                upload_templates(token, templatePathofET, templatePathofMysql)
	            }
	        }*/

	        stage('create mysql app'){
	            container('qe-testing-runner'){
	                create_apps(token, "${appName}-mysql", templateNameofMysql, mysqlTemplateParameters)
	            }
	        }
	        stage('create et app'){
	            container('qe-testing-runner'){
	                create_apps(token, appName, templateNameofET, etTemplateParameters)
	            }
	        }
	        stage('build mysql app'){
	            container('qe-testing-runner'){
	                build_bc_and_track_build(token, 20, "${appName}-mysql")
	            }
	        }
	        stage('deploy mysql app'){
	            container('qe-testing-runner'){
	                deploy_dc_and_track_deployment(token, 5, "${appName}-mysql")
	            }
	        }
	        stage('build et app'){
	            container('qe-testing-runner'){
	                build_bc_and_track_build(token, 20, "${appName}-bc")
	            }
	        }
	        stage('deploy et app'){
	            container('qe-testing-runner'){
	                deploy_dc_and_track_deployment(token, 5, "${appName}-rails")
	            }
	        }
	        if(qeTesting=='true'){
	            try { 
	                stage('TS2 testing preparation'){
	                    container('qe-testing-runner'){
	                        
	                        def mysqlPod = get_pod_name_for_dc(token, "${appName}-mysql")
	                        def etPod = get_pod_name_for_dc(token, "${appName}-rails")

	                        import_sql_files_to_db(token, mysqlPod, DB_FILE, MSYQL_USER, MYSQL_PASSWORD)
	                        def db_migration_cmd = "bundle exec rake db:migrate"
	                        run_cmd_against_pod(token, etPod, db_migration_cmd)

	                        disable_sending_qpid_message(token, etPod)

	                        if(casesTags.contains(' @umb')){
	                            specify_cucumber_umb_broker(token, etPod)
	                        }

	                        restart_et_service(token, etPod)

	                    }
	                }             
	                stage('run TS2 testing'){
	                    container('qe-testing-runner'){
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

	                        def etPod = get_pod_name_for_dc(token, "${appName}-rails")
	                        run_ts2_testing(token, appName, etPod, casesTags)
	                    }
	                } //stage
	            } //try
	            catch(Exception e) {
	                echo "Failed to do QE testing ..."
	            }
	            finally{
	                archiveArtifacts '**/cucumber-report*.json'
	                cucumber fileIncludePattern: "**/cucumber-report*.json", sortingMethod: "ALPHABETICAL"
	            }
	        } //if
	    } //node
	  } //container
}