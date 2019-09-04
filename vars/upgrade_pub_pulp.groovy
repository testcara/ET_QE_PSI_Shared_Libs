def call(String pubServer, String pulpServer, String pulpDockerServer, String pubJenkinsBuild="", String pulpBuildForRPM="", String pulpRPMBuild="",
    String pulpCDNDistributorBuild="", String pulpBuildForDocker="", String pulpDockerBuild=""){

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

            git 'https://gitlab.infra.prod.eng.rdu2.redhat.com/yuzheng/ansible-pub-qe.git'

            sh "echo $pub_server > pub_server"
            sh "echo $pulp_rpm_server > pulp_rpm_server"
            sh "echo $pulp_docker_server > pulp_docker_server"
            sh "echo $pub_jenkins_build > pub_jenkins_build"
            sh "echo $pulp_build_for_rpm > pulp_build_for_rpm"
            sh "echo $pulp_rpm_build > pulp_rpm_build"
            sh "echo $pulp_cdn_distributor_build > pulp_cdn_distributor_build"
            sh "echo $pulp_build_for_docker > pulp_build_for_docker"
            sh "echo $pulp_docker_build > pulp_docker_build"

            sh '''
            user_id=$(id | cut -d " " -f 1 | cut -d "=" -f 2 | tr -d ' ')
            echo "jenkins:x:${user_id}:0::/home/jenkins:/bin/bash" >> /etc/passwd

            wget http://github.com/testcara/RC_CI/archive/master.zip
            unzip master.zip

            export CI3_WORKSPACE="${WORKSPACE}"
            export pub_server=$(cat ${CI3_WORKSPACE}/pub_server)
            export pulp_rpm_server=$(cat ${CI3_WORKSPACE}/pulp_rpm_server)
            export pulp_docker_server=$(cat ${CI3_WORKSPACE}/pulp_docker_server)
            export pub_jenkins_build=$(cat ${CI3_WORKSPACE}/pub_jenkins_build)
            export pulp_build_for_rpm=$(cat ${CI3_WORKSPACE}/pulp_build_for_rpm)
            export pulp_rpm_build=$(cat ${CI3_WORKSPACE}/pulp_rpm_build)
            export pulp_cdn_distributor_build=$(cat ${CI3_WORKSPACE}/pulp_cdn_distributor_build)
            export pulp_build_for_docker=$(cat ${CI3_WORKSPACE}/pulp_build_for_docker)
            export pulp_docker_build=$(cat ${CI3_WORKSPACE}/pulp_docker_build)

            cd RC_CI-master/auto_testing_CI/
            ./upgrade_pub_pulp_psi.sh
            #./prepare_pulp_pulp_db_and_services.sh
            '''
            }
        }
    }
}
