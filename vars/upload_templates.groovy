def call(String token, String template){
	echo '--- Upload template --->'
    openshift.create(template)
} //call
