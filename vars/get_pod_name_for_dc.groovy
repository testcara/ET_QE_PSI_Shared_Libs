def call(String token, String dcName){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
      openshift.withProject('errata-qe-test'){
      	echo "---> Get pod name for dc ${dcName} ..."
      	print openshift.selector("dc", dcName).related('pods').names()[0].split("/")[1]
        return openshift.selector("dc", dcName).related('pods').names()[0].split("/")[1]
      }
    }
}
