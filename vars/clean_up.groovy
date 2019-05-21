def call(String token, String name, String type){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
            echo "---> Cleanup ..."
            switch(type) {
                case 'app':
                openshift.selector("all", [ app : "$name" ]).delete()
                break
                case 'template':
                openshift.selector("all", [ app : "$name" ]).delete()
                break
            } //switch
        } //project
    } //cluster
}
