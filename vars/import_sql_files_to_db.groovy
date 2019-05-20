def call(String token, String podName, String sqlFile, String mysqlUser, String mysqlPassword){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
    openshift.withProject('errata-qe-test'){
      sh "oc exec ${podName} -i -- mysql -u${mysqlUser} -p${mysqlPassword} -Derrata < ${sqlFile}"
    }
  }
}
