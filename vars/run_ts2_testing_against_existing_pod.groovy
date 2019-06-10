def call(String token, String appName, String casesFeatures){

	echo "---> Now, you are using the ET pipeline shared lib ..."

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
        claimName: 'brew-qa',
        mountPath: '/mnt/redhat')
    ])
	{
	    node(runner) {
	    	try{
	            stage('TS2 testing preparation'){
	                container('qe-testing-runner'){
                        def cmd1="oc get pods | grep ${appName}-mysql | grep -v build | cut -d ' ' -f 1"
                        def mysqlPod = sh(returnStdout: true, script: cmd1).trim()
                        echo "Got mysqlPod: ${mysqlPod}"

                        def cmd2="oc get pods | grep ${appName}-rails | grep -v build | cut -d ' ' -f 1"
                        def etPod = sh(returnStdout: true, script: cmd2).trim()
                        echo "Got etPod: ${etPod}"

                        import_sql_files_to_db(token, mysqlPod, DB_FILE, MSYQL_USER, MYSQL_PASSWORD)
                        def db_migration_cmd = "bundle exec rake db:migrate"
                        run_cmd_against_pod(token, etPod, db_migration_cmd)

                        disable_sending_qpid_message(token, etPod)

                        if(casesFeatures.contains('UMB')){
                            specify_cucumber_umb_broker(token, etPod)
                        }

                        restart_et_service(token, etPod)

                        echo "Add the pulp configuration files to runner"
                        sh """
                        mkdir ~/.rcm
                        cp /tmp/pulp_configs/.rcm/pulp-environments.json ~/.rcm/
                        """

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

                        def cmd="oc get pods | grep ${appName}-rails | grep -v build | cut -d ' ' -f 1"
                        def etPod = sh(returnStdout: true, script: cmd).trim()
                        echo "Got etPod: ${etPod}"
                        run_ts2_testing(token, appName, etPod, casesFeatures)
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
	    } //node
	  } //container
} //call