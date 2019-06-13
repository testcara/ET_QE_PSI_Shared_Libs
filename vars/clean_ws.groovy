def call(){
	sh '''
	deleteDir()
	find . -name "*.log" | xargs rm -rf
	'''
}