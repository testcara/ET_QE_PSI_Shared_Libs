def call(String token, Integer time, String dcName ){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
            echo "--- Deploy dc: ${dcName} --->"
            sh "oc rollout latest ${dcName}"
            sh "echo $dcName > dcName" 
            sh '''
            dcName=$(cat dcName)
            # let us wait 5 mins
            for i in {1..30}
            do
                sleep 10 # 10 seconds
                status=$(oc get pods | grep ${dcName} | grep -v build | grep -v deploy | awk "{print $3}")
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
            /*
            def goodBuildStatus = (String[]) ['Running', 'Pending', 'Complete']
		    def dcSelector = openshift.selector("dc", dcName)

		    timeout(time) { 
		    	sleep(10)
		        openshift.selector("dc", dcName).related('pods').untilEach(1) {
		            if (it.object().status.phase == "Running" )
		            {
		                echo "---> Deploy Complete ..."
		                return true
		            }
		        } //each
		    } // timeout
		    */
        } //project
    } //cluster
} //call