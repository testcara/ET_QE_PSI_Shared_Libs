def call(String token, String dcName){
    def pod = sh "oc get pods | grep ${dcName} | grep -v build | cut -d ' ' -f 1"
    return pod
}
