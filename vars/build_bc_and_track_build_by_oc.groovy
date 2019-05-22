def call(String token, Integer time, String bcName){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
            sh "echo $bcName > bcName"
            sh "oc start-build $bcName"
            sh "echo $bcName > bcName" 
            sh '''
            bcName=$(cat bcName)
            # let us wait 30 mins
            for i in {1..60}
            do
                sleep(30) # 30 seconds
                status=$(oc get build | grep ${bcName} | awk "{print $4}")
                if [[ ${status} == "Complete" ]]
                then
                  echo "---> Build complete ..."
                  exit 0
                elif [[ ${status} =~ "Failed" ]]
                then
                  echo "---> Build has been failed ..."
                  exit 1
                else
                  echo "---> Still running ..."
                fi
            done
            '''

            /*
            def goodBuildStatus = (String[]) ['Running', 'Pending', 'Complete']
            def bcSelector = openshift.selector("bc", bcName)
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
            */
        } //project
    } //cluster
} //call