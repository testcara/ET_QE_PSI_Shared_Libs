def call(String token, String name, String type){
        echo "---> Cleanup ..."
        switch(type) {
            case 'app':
            echo "---> Clean apps: $name ..."
            sh "echo ${name} > name"
            sh '''
            app_name=$(cat name)
            oc delete all -l app=${app_name} --ignore-not-found=true
            '''
            break
            case 'template':
            echo "---> Clean template: $name ..."
            sh "echo ${name} > name"
            sh '''
            template_name=$(cat name)
            oc delete template ${template_name} --ignore-not-found=true
            '''
            break
        } //switch
}
