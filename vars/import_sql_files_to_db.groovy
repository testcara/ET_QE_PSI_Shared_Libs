def call(String token, String sqlFile, String podName, String mysqlUser, String MysqlPassword){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
    openshift.withProject('errata-qe-test'){
      sh "oc exec ${podName} -i -- mysql -u${mysqlUser} -p${MysqlPassword} -Derrata < ${sqlFile}"
    }
  }
}
