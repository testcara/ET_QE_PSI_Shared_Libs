def call(String token, Integer time, String dcName ){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
            echo "--- Deploy dc: ${dcName} --->"
            def goodBuildStatus = (String[]) ['Running', 'Pending', 'Complete']
		    def dcSelector = openshift.selector("dc", dcName)
		    dcSelector.rollout().latest()
		    timeout(time) { 
		        openshift.selector("dc", dcName).related('pods').untilEach(1) {
		            if (it.object().status.phase == "Running" )
		            {
		                echo "---> Deploy Complete ..."
		                return true
		            }
		        } //each
		    } // timeout
        } //project
    } //cluster
} //call