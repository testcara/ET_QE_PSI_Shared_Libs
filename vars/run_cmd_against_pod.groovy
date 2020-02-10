def call(String projectName, String podName, String command){
      sh "oc exec ${podName} -n ${projectName} -i -- ${command}"
}
