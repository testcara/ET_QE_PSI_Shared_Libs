def call(String token, String app_name, String etPod, String casesTags){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
    openshift.withProject('errata-qe-test'){
    sh "echo ${app_name} > app_name"
    sh "echo ${etPod} > et_pod"
    sh "echo ${casesTags} > cases_tags"

    sh '''
      reset_testing_host(){
        default_host="et-system-test-qe-01.usersys.redhat.com"
        env_path=$(find . -name 'env.yml')
        sed -i "s/${default_host}/${1}.cloud.paas.psi.redhat.com/g" ${env_path}
      }

      specify_runner_umb_for_cucumber_umb_cases(){
        echo "---> Setting umb for umb cases ..."
        umb_path=$(find . -name 'umb.rb')
        sed -i "s/umb-qe/cucumber-umb-qe/g" ${umb_path}
      }

      pod_name=$(cat et_pod)
      app_name=$(cat app_name)
      cases_tags=$(cat cases_tags)

      if [[ ${cases_tags} =~ "@umb" ]] && [[ ! ${cases_tags} =~ "~@umb" ]]
      then
        specify_runner_umb_for_cucumber_umb_cases
      fi

      reset_testing_host ${app_name}

      gem_file_path=$(find . -name "Gemfile.lock" | sed "s/Gemfile.lock//")
      cd ${gem_file_path}

      RAILS_ENV=test bundle install --path=/opt/rh/rh-ruby22/root/usr/local/bin
      cucumber_cmd="ET_POD=${pod_name} RUN_ON_PSI=1 TEST_ENV=qe_01 BZ_ADMIN_PASSWD=1HSSQE@redhat bundle exec cucumber -p remote"
      cucumber_report="--format json_pretty --strict -o cucumber-report-${app_name}.json"
      features_dir="features/remote"
      rerun_report="-f pretty -f rerun --out rerun.txt"
      rerun_cmd="@rerun.txt"
      echo "---> Write the cucumber testing script ..."
      echo "${cucumber_cmd} ${cases_tags} ${cucumber_report} ${rerun_report} ${features_dir} || ${cucumber_cmd} ${rerun_cmd}" > cucumber_report.sh
      chmod +x cucumber_report.sh
      ./cucumber_report.sh
      '''
    } //project
  } //cluster

}
