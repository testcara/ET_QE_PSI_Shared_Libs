def call(String token, String dcName){
        sh "echo $dcName > dcName"
        sh '''
            dcName=$(cat dcName)
            # let us wait 5 mins
            for i in {1..5}
            do
                sleep 60 # 10 seconds
                status=$(oc get pods | grep ${dcName} | grep -v build | grep -v deploy | awk "{print $3}")
                if [[ ${status} =~ "Running" ]]
                then
                  echo "---> Deployment complete ..."
                  exit 0
                elif [[ ${status} =~ "Failed" ]] || [[ ${status} =~ "Error" ]]
                then
                  echo "---> Deployment has been failed ..."
                  exit 1
                else
                  echo "---> Still running ..."
                  if [[ $i -eq 5 ]]
                  then
                    exit 1
                  fi
                fi
            done
           '''
}//call
