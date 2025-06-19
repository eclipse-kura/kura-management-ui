podTemplate(inheritFrom: 'basic', yaml: '''
spec:
  containers:
  - name: "jnlp"
''')
{
    node {
        properties([
            disableConcurrentBuilds(abortPrevious: true),
            buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '2', daysToKeepStr: '', numToKeepStr: '5')),
            gitLabConnection('gitlab.eclipse.org'),
            [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
            [$class: 'JobLocalConfiguration', changeReasonComment: '']
        ])

        deleteDir()

        stage('prepare') {
            dir('kura-management-ui') {
                checkout scm
            }

        }


        stage('Build kura-management-ui') {
            timeout(time: 2, unit: 'HOURS') {
                dir('kura-management-ui') {
                    withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
                        sh 'mvn clean install'
                    }
                }
            }
        }
    }
}
