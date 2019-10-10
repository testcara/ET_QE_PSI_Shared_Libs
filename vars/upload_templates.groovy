def call(String token, String template){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
	        echo '--- Upload template --->'
	        openshift.create(template)
    	} //project
    } //cluster
} //call
