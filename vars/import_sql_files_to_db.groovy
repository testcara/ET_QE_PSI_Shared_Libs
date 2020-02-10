def call(String projectName, String podName){
      //sh "oc exec ${podName} -i -- mysql -u${mysqlUser} -p${mysqlPassword} -Derrata < ${sqlFile}"
      // for mariadb, we need to specify the absolute path of mysql and import the data without password
      sh "echo $podName > podName"
      sh "echo $projectName > projectName"
      sh '''
      podName=$(cat podName)
      projectName=$(cat projectName)
      wget ftp://ftp-et-qe.usersys.redhat.com/errata.latest.sql
      oc exec ${podName} -n ${projectName} -i -- /opt/rh/rh-mariadb102/root/usr/bin/mysql -uroot -Derrata < errata.latest.sql
      '''
}
