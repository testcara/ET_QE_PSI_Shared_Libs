def call(String token, Integer time, String bcName){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
            def goodBuildStatus = (String[]) ['Running', 'Pending', 'Complete']
            def bcSelector = openshift.selector("bc", bcName)
            bcSelector.startBuild()
            def builds= bcSelector.related('builds')
            timeout(time) {
                // Checking watch output and running watch closure again in 250ms
                builds.untilEach(1) {
                    def status = it.object().status.phase
                    if ( goodBuildStatus.contains(status) == false ) {
                        throw new Exception("Build failed")
                    } //if
                    if ( status == 'Complete' )
                    {
                        echo "---> Build Complete ..."
                        return true
                    } //if
                } //each
            } //timeout
        } //project
    } //cluster
} //call