def call(){
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
        git config --global http.sslVerify false
        git clone https://gitlab.infra.prod.eng.rdu2.redhat.com/ansible-playbooks/errata-tool-playbooks.git
        wget http://github.com/testcara/RC_CI/archive/master.zip
        sleep 7200
      '''
      }
    } // node
  } //pod
} //call
