def call(String token, String appName, String casesFeatures){

	echo "---> Now, you are using the ET pipeline shared lib ..."

	def RUN_USER = '1058980001'
	def MSYQL_USER = "root"
	def MYSQL_PASSWORD = "arNdk123_"
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
        claimName: 'pvc-errata-qe-test-mnt-redhat',
        mountPath: '/mnt/redhat')
    ])
	{
	    node(runner) {
	    	try{
	            stage('TS2 testing preparation'){
	                container('qe-testing-runner'){

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
