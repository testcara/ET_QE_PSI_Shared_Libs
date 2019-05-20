def call(String token, String app_name, String templateName, String templateParameters){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
	    openshift.withProject('errata-qe-test'){
			echo '--- Create app ${app_name} from the ${templateName} --->'
			def objectsName
			if(${app_name}.toLowerCase().contains('mysql')){
				objectsName = (String[]) ["is/${app_name}", "bc/${app_name}", "dc/${app_name}", "svc/${app_name}"]
			} else{
				objectsName = (String[]) ["is/${app_name}-s2i", "is/${app_name}-basic", "bc/${app_name}-bc", "dc/${app_name}-rails", "route/${app_name}-route", "svc/${app_name}-svc"]
			}

		    def templateGeneratedSelector = openshift.selector(objectsName)
		    def objectModels = openshift.process(templateName, templateParameters)
		    def objects
		    def verb
		    def objectsGeneratedFromTemplate = templateGeneratedSelector.exists()
		    if (!objectsGeneratedFromTemplate) {
		        verb = "Created"
		        objects = openshift.create(objectModels)
		    } else {
		        verb = "Found"
		        objects = templateGeneratedSelector
		    }
		    objects.withEach {
		        echo "${verb} ${it.name()} from template with labels ${it.object().metadata.labels}"
		    }
		    return objects
		} //project
	} //cluster
} //call