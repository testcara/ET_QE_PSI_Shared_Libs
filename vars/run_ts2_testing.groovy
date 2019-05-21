def call(String token, String app_name, String etPod, String casesTags ){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
    openshift.withProject('errata-qe-test'){
    sh "echo ${app_name} > app_name"
    sh "echo ${etPod} > et_pod"
    sh "echo ${casesTags} > cases_tags"

    sh '''
    pwd
    ls
    reset_testing_host(){
      default_host="et-system-test-qe-01.usersys.redhat.com"
      sed -i "s/${default_host}/${1}.cloud.paas.psi.redhat.com/g" features/remote/config/env.yml
    }

      specify_runner_umb_for_cucumber_umb_cases(){
        umb_config="features/remote/support/umb.rb"
        sed -i "s/umb-qe/cucumber-umb-qe/g" $umb_config
      }

      if [ "${casesTags}" =~ '@umb' ]
      then
        specify_runner_umb_for_cucumber_umb_cases
      fi


      pod_name=$(cat et_pod)
      app_name=$(cat app_name)
      cases_tags=$(cat cases_tags)

      reset_testing_host ${app_name}

      RAILS_ENV=test bundle install
      cucumber_cmd="TEST_ENV=qe_01 BZ_ADMIN_PASSWD=1HSSQE@redhat bundle exec cucumber -p remote"
      cucumber_report="--format json_pretty --strict -o cucumber-report-${app_name}.json features/remote "
      ET_POD=${pod} RUN_ON_PSI=1 ${cucumber_cmd} ${cases_tags} ${cucumber_report}

      '''
    } //project
  } //cluster

}
