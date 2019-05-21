def call(String token, String name, String type){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
            echo "---> Cleanup ..."
            switch(type) {
                case 'app':
                echo "---> Clean apps: $name ..."
                openshift.selector("all", [ app : "$name" ]).delete()
                break
                case 'template':
                echo "---> Clean template: $name ..."
                openshift.selector("template", "$name").delete()
                break
            } //switch
        } //project
    } //cluster
}
