def call(String token, String podName, String command){
      sh "oc exec ${podName} -i -- ${command}"
}
