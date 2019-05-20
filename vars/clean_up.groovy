def call(String token, String app_name, String templateNameofET, String templateNameofMysql){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
        openshift.withProject('errata-qe-test'){
            echo "--- Delete apps --->"

            def etapp_names = ["${app_name}-1", "${app_name}-2", "${app_name}-3"]
            def mysqlapp_names = ["${app_name}-1-mysql", "${app_name}-2-mysql", "${app_name}-3-mysql"]
            etapp_names.each { app ->
                openshift.selector("all", [ app : "$app" ]).delete()
            }
            mysqlapp_names.each { app ->
                openshift.selector("all", [ app : "$app" ]).delete()
            }
            def exist1 = openshift.selector("template", "$templateNameofET").exists()
            if (exist1) {
                echo "--- Delete ET template --->"
                openshift.selector("template", "$templateNameofET").delete()
            } //if
            def exist2 = openshift.selector("template", "$templateNameofMysql").exists()
            if (exist2) {
                echo "--- Delete Mysql template --->"
                openshift.selector("template", "$templateNameofMysql").delete()
            }
            } //project
    } //cluster
}
