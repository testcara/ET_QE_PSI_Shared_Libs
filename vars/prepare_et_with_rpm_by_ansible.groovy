def call(String et_server, String et_version=""){
  sh "echo export ET_Testing_Server=$et_server > /home/jenins/.bashrc"
  sh "echo export et_build_name_or_id=$et_version >> /home/jenkins/.bashrc"
  def runner = "mypod-${UUID.randomUUID().toString()}"
  podTemplate(label: runner,
  containers: [
  containerTemplate(
    name: 'et-ansible-runner',
    image: 'docker-registry.upshift.redhat.com/errata-qe-test/et_ansible_runner:latest',
    alwaysPullImage: true,
    command: 'cat',
    ttyEnabled: true,
    envVars: [
      envVar(key: 'GIT_SSL_NO_VERIFY', value: 'true')
    ]

    )]
  )
  {
    node(runner) {
      script{
        sh '''
        source /home/jenkins/.bashrc
        git config --global http.sslVerify false
        git clone https://gitlab.infra.prod.eng.rdu2.redhat.com/ansible-playbooks/errata-tool-playbooks.git
        wget http://github.com/testcara/RC_CI/archive/master.zip
        unzip master.zip
        export WORKSPACE=`pwd`
        RC_CI-master/auto_testing_CI/prepare_et_server_with_rpm_by_ansible.sh
        '''
      }
    } // node
  } //pod
} //call
