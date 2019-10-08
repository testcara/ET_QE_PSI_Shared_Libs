def call(String token, String appName, String appParameters, String repoImage){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
	    openshift.withProject('errata-qe-test'){
	    	sh "echo $appName > appName"
	    	sh "echo $appParameters > appParameters"
	    	sh "echo $repoImage > repoImage"
	    	sh '''
	    	app_name=$(cat appName)
	    	repoImage=$(cat repoImage)
	    	appParameters=$(cat appParameters)
		echo oc new-app --name=${app_name} ${appParameters} ${repoImage}
	    	oc new-app --name=${app_name} ${appParameters} ${repoImage}
	    	'''
		} //project
	} //cluster
} //call
