def call(String pubServer, String pulpServer, String pulpDockerServer) {
    def runner_1 = "mypod-${UUID.randomUUID().toString()}"
    podTemplate(label: runner_1,
        containers: [
            containerTemplate(
                name: 'qe-pub-pulp-testing-runner',
                    image: 'docker-registry.upshift.redhat.com/errata-qe-test/et_python2_runner:latest',
                        command: 'cat',
                        ttyEnabled: true,
                        alwaysPullImage: true,
                        envVars: [
                            envVar(key: 'GIT_SSL_NO_VERIFY', value: 'true')
                            ]
                        )]
    )
    {
        node(runner_1) {
            properties([
                parameters([
                     string(name: 'pub_server', defaultValue: pubServer),
                     string(name: 'pulp_rpm_server', defaultValue: pulpServer),
                     string(name: 'pulp_docker_server', defaultValue: pulpDockerServer)
                     ]
                    )
            ])

            container('qe-pub-pulp-testing-runner') {
            sh '''git config --global http.sslVerify false'''
            git 'https://gitlab.infra.prod.eng.rdu2.redhat.com/yuzheng/ansible-pub-qe.git'
            sh '''
            git clone https://gitlab.cee.redhat.com/wlin/rc_ci.git

            cd rc_ci/auto_testing_CI/
            export CI3_WORKSPACE="${WORKSPACE}"
            ./initial_pub_pulp_psi.sh
            ./prepare_pulp_pulp_db_and_services.sh
            '''
            }
        }
    }
}
