def call(String token, String dcName){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
      openshift.withProject('errata-qe-test'){
      	def pod = sh "oc get pods | grep ${dcName} | cut -d ' ' -f 1"
      	return pod
      }
    }
}
