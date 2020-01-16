def call(String token, String app_name, String etPod, String casesFeatures, String app_svc){
    sh "echo ${app_svc} > app_svc"
    sh "echo ${app_name} > app_name"
    sh "echo ${etPod} > et_pod"
    sh "echo \"${casesFeatures}\" > cases_features"

    sh '''
      reset_testing_host(){
        default_host="et-system-test-qe-01.usersys.redhat.com"
        env_path=$(find . -name 'env.yml')
        sed -i "s/${default_host}/${1}/g" ${env_path}
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

      svc=$(cat app_svc)
      reset_testing_host ${svc}

      gem_file_path=$(find . -name "Gemfile.lock" | sed "s/Gemfile.lock//")
      cd ${gem_file_path}
      sed -i "/gem 'launchy'/a\\  gem 'capybara-screenshot'" Gemfile
      sed -i "/gem 'capybara-screenshot'/a\\  gem 'faraday'" Gemfile
      sed -i "/gem 'faraday'/a\\  gem 'faraday_middleware'" Gemfile
      sed -i "s/require 'jira'/require 'jira-ruby'/" features/remote/support/jira.rb
      # The current cases of the release branch cannot test the release source code well.
      # We will meet the following error
      # uninitialized constant Minitest::Assertion (NameError)
      # The following command will fix the problem
      sed -i 's/test\\/unit/minitest/g' features/remote/support/env.rb
      sed -i "s/-- bundle exec rails console \\/tmp\\/rails-#{date}\\/Autotasks.txt/-- bash -c 'RAILS_ENV=staging bundle exec rails console \\/tmp\\/rails-#{date}\\/Autotasks.txt'/g" features/remote/support/errata_rails_console.rb
      sed -i "s/-- bundle exec rails runner \\/tmp\\/rails-#{date}\\/Autotasks.txt/-- bash -c 'RAILS_ENV=staging bundle exec rails runner \\/tmp\\/rails-#{date}\\/Autotasks.txt'/g" features/remote/support/errata_rails_console.rb
      sed -i "s/oc rsync/oc rsync -n c3i-carawang-123/g" features/remote/support/errata_rails_console.rb
      sed -i "s/oc exec/oc exec -n c3i-carawang-123/g" features/remote/support/errata_rails_console.rb

      RAILS_ENV=test bundle install --path=/opt/rh/rh-ruby22/root/usr/local/bin
      cucumber_cmd="ET_POD=${pod_name} RUN_ON_PSI=1 TEST_ENV=qe_01 ET_ADMIN_PASSWD=redhat BZ_ADMIN_PASSWD=1HSSQE@redhat JBOSS_JIRA_PASSWD=errata-qe bundle exec cucumber -p remote"
      cucumber_report="--format json_pretty --strict -o cucumber-report-${app_name}.json"
      features_dir="features/remote"
      rerun_report="-f pretty -f rerun --out rerun.txt"
      rerun_cmd="@rerun.txt --format json_pretty --strict -o rerun.json"
      echo "---> Write the cucumber testing script ..."
      echo "${cucumber_cmd} ${cases_features} ${cucumber_report} ${rerun_report} ${features_dir} || ${cucumber_cmd} ${rerun_cmd}" > cucumber_report.sh
      chmod +x cucumber_report.sh
      ./cucumber_report.sh || true
      # after the rerun, let us use the paraser to merge the rerun.json and cucumber-report.json
      if [[ $(find . -name "*rerun*.json") =~ "rerun" ]]
      then
        echo "Have got the rerun json and call the rerun parser now!"
        cp /tmp/TS2_db/cucumber_rerun_parser.py .
        python3 cucumber_rerun_parser.py --rerun-report=rerun.json --origin-report=cucumber-report-${app_name}.json --new-report=${app_name}-new-report.json
      else
        echo "No rerun json, we would not call the rerun parser!"
      fi
      if [[ $(find . -name "*new-report*.json") =~ "new-report" ]]
      then
        echo "Have got the new report and delete the obsolete one now!"
        rm -rf cucumber-report-${app_name}.json rerun.json
        mv ${app_name}-new-report.json cucumber-report-${app_name}.json
      else
        echo "No new report json, we would keep the origin reports!"
      fi
      '''
}
