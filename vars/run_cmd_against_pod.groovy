def call(String token, String podName, String command){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
    openshift.withProject('errata-qe-test'){
      sh "oc exec ${podName} -i -- ${command}"
    }
  }
}
