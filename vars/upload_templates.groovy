def call(String token, String templatePathofET, String templatePathofMysql){
	openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
	        echo '--- Upload ET template --->'
	        openshift.create(templatePathofET)
	        echo '--- Upload Mysql template --->'
	        openshift.create(templatePathofMysql)
    	} //project
    } //cluster
} //call