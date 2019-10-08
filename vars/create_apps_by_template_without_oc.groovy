def call(String token, String name, String template, String templateParameters){
    openshift.withCluster('https://paas.psi.redhat.com', token) {
	    openshift.withProject('errata-qe-test'){
	    	def parameters = "-p=APP_NAME=${name} ${templateParameters}"
			echo "--- Create app $name from the $template --->"
			echo "--- With parameters: $parameters ...>"
			def objectsName
			if(name.toLowerCase().contains('mysql')){
				objectsName = (String[]) ["is/${name}", "bc/${name}", "dc/${name}", "svc/${name}"]
			} else{
				objectsName = (String[]) ["is/${name}-s2i", "is/${name}-basic", "bc/${name}-bc", "dc/${name}-rails", "route/${name}-route", "svc/${name}-svc"]
			}
			echo "---> the following objects will be created:"
			echo objectsName.toString()


		    def templateGeneratedSelector = openshift.selector(objectsName)
		    def objectModels = openshift.process(template, parameters)
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
