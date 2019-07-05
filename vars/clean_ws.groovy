def call(){
	sh '''
	rm -rf *
	find . -name "*.log" | xargs rm -rf
	'''
}
