def call(String token, String podName, String sqlFile, String mysqlUser, String mysqlPassword){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
    openshift.withProject('errata-qe-test'){
      //sh "oc exec ${podName} -i -- mysql -u${mysqlUser} -p${mysqlPassword} -Derrata < ${sqlFile}"
      // for mariadb, we need to specify the absolute path of mysql and import the data without password
      sh "oc exec ${podName} -i -- /opt/rh/rh-mariadb102/root/usr/bin/mysql -u${mysqlUser}  -Derrata < ${sqlFile}" 
    }
  }
}
