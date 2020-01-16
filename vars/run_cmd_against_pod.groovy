def call(String token, String podName, String command){
      sh "oc exec ${podName} -n c3i-carawang-123 -i -- ${command}"
}
