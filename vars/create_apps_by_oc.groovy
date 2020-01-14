def call(String token, String name, String template, String templateParameters){
		sh "echo $name > name"
	    sh "echo $templateParameters > templateParameters"
	    sh "echo $template > template"
	    sh '''
	    app_name=$(cat name)
	    template=$(cat template)
	    templateParameters=$(cat templateParameters)
	    templateParameters="-p=APP_NAME=${app_name} ${templateParameters}"
	    oc process ${template} ${templateParameters} | oc create -f -
	    '''
} //call
