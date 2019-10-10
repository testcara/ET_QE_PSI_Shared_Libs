def call(String token, String template){
//def call(String token, String template, String templatePathofMysql){
	openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
	        echo '--- Upload template --->'
	        openshift.create(template)
    	} //project
    } //cluster
} //call