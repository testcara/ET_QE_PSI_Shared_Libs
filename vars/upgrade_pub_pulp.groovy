def call(String pubServer, String pulpServer, String pulpDockerServer, String pubJenkinsBuild="", String pulpBuildForRPM="", String pulpRPMBuild="",
    String pulpCDNDistributorBuild="", String pulpBuildForDocker="", String pulpDockerBuild=""){

 params.pub_jenkins_build, params.pulp_build_for_rpm, params.pulp_rpm_build, params.pulp_cdn_distributor_build, params.pulp_build_for_docker, params.pulp_docker_build
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
                     string(name: 'pulp_docker_server', defaultValue: pulpDockerServer),
                     string(name: 'pub_jenkins_build', defaultValue: pubJenkinsBuild),
                     string(name: 'pulp_build_for_rpm', defaultValue: pulpBuildForRPM),
                     string(name: 'pulp_rpm_build', defaultValue: pulpRPMBuild),
                     string(name: 'pulp_cdn_distributor_build', defaultValue: pulpCDNDistributorBuild),
                     string(name: 'pulp_build_for_docker', defaultValue: pulpBuildForDocker),
                     string(name: 'pulp_docker_build', defaultValue: pulpDockerBuild)
                     ]
                    )
            ])

            container('qe-pub-pulp-testing-runner') {
            sh '''git config --global http.sslVerify false'''
            sh "echo "
            git 'https://gitlab.infra.prod.eng.rdu2.redhat.com/yuzheng/ansible-pub-qe.git'
            sh '''
            wget http://github.com/testcara/RC_CI/archive/master.zip
            unzip master.zip
            cd RC_CI-master/auto_testing_CI/
            echo ${WORKSPACE}
            export CI3_WORKSPACE="${WORKSPACE}"
            sleep 3600
            ./upgrade_pub_pulp_psi.sh
            ./prepare_pulp_pulp_db_and_services.sh
            '''
            }
        }
    }
}
