def call(String token, String podName){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
    openshift.withProject('errata-qe-test'){
      //sh "oc exec ${podName} -i -- mysql -u${mysqlUser} -p${mysqlPassword} -Derrata < ${sqlFile}"
      // for mariadb, we need to specify the absolute path of mysql and import the data without password
      sh "echo $podName > podName"
      sh '''
      podName=$(cat podName)
      wget ftp://ftp-et-qe.usersys.redhat.com/errata.latest.sql
      oc exec ${podName} -i -- /opt/rh/rh-mariadb102/root/usr/bin/mysql -uroot -Derrata < errata.latest.sql
      '''
    }
  }
}
