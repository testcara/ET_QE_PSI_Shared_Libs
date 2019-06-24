def call(){
  def runner= "mypod-${UUID.randomUUID().toString()}"
  podTemplate(label: runner,
    containers: [
    containerTemplate(
        name: 'qe-testing-runner',
        image: 'docker-registry.upshift.redhat.com/errata-qe-test/qe_testing_upshift_runner:latest',
        alwaysPullImage: true,
        command: 'cat',
        ttyEnabled: true,

        envVars: [
            envVar(key: 'GIT_SSL_NO_VERIFY', value: 'true')
        ]

        )],
    volumes: [
    persistentVolumeClaim(
        claimName: 'et-qe-testing-mysql',
        mountPath: '/tmp/TS2_db/'),
    persistentVolumeClaim(
        claimName: 'pvc-errata-qe-test-mnt-redhat',
        mountPath: '/mnt/redhat')
    ])
  {
      node(runner) {
      try{
              stage('TS2 testing preparation'){
                  container('qe-testing-runner'){
                        sh '''
                        git clone https://code.engineering.redhat.com/gerrit/errata-rails
                        cd errata-rails
                        git checkout develop
                        RAILS_ENV=test bundle install --path=/opt/rh/rh-ruby22/root/usr/local/bin
                        sleep 7200
                        '''
                  } //container
              } //stage
            } //try
            catch(Exception e) {
                echo "Failed to do QE testing ..."
            }
      } //node
    } //container
} //call
