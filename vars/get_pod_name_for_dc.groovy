def call(String token, String dcName){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
      openshift.withProject('errata-qe-test'){
        return openshift.selector("dc", dcName).related('pods').names()[0]
      }
    }
}
