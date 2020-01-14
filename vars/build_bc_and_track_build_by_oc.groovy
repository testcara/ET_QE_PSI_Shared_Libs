def call(String token, Integer time, String bcName){
		sh "echo $bcName > bcName"
        sh "oc start-build $bcName"
        sh "echo $bcName > bcName" 
        sh '''
        bcName=$(cat bcName)
        # let us wait 30 mins
        for i in {1..60}
        do
            sleep 30 # 30 seconds
            status=$(oc get build | grep ${bcName} | awk "{print $4}")
            if [[ ${status} =~ "Complete" ]]
            then
				echo "---> Build complete ..."
                exit 0
            elif [[ ${status} =~ "Failed" ]]
            then
                echo "---> Build has been failed ..."
                exit 1
            else
                echo "---> Still running ..."
            fi
        done
        '''
} //call
