def call(){
	sh '''
	find . -name "*.log" | xargs rm -rf
	'''
}