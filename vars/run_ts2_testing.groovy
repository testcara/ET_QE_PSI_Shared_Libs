def call(String token, String app_name, String etPod, String casesFeatures){
  openshift.withCluster('https://paas.psi.redhat.com', token) {
    openshift.withProject('errata-qe-test'){
    sh "echo ${app_name} > app_name"
    sh "echo ${etPod} > et_pod"
    sh "echo \"${casesFeatures}\" > cases_features"

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
      cases_features=$(cat cases_features)

      if [[ ${cases_features} =~ "UMB" ]]
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
      rerun_cmd="@rerun.txt --format json_pretty --strict -o rerun.json"
      echo "---> Write the cucumber testing script ..."
      echo "${cucumber_cmd} ${cases_features} ${cucumber_report} ${rerun_report} ${features_dir} || ${cucumber_cmd} ${rerun_cmd}" > cucumber_report.sh
      chmod +x cucumber_report.sh
      ./cucumber_report.sh || true
      # after the rerun, let us use the paraser to merge the rerun.json and cucumber-report.json
      cp /tmp/TS2_db/cucumber_rerun_parser.py .
      python3 cucumber_rerun_parser.py --rerun-report=rerun.json --origin-report=cucumber-report-${app_name}.json --new-report=${app_name}-new-report.json
      rm -rf cucumber-report-${app_name}.json rerun.json
      mv ${app_name}-new-report.json cucumber-report-${app_name}.json
      '''
    } //project
  } //cluster
}
