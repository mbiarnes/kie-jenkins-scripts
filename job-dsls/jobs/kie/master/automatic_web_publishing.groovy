/**
 * job that publishes automatically the ${repo}-website
 */

import org.kie.jenkins.jobdsl.Constants

// creation of folder
folder("KIE")
folder("KIE/master")
folder("KIE/master/webs")
def folderPath="KIE/master/webs"

def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def mainBranch=Constants.BRANCH
def AGENT_LABEL="kie-rhel7 && kie-mem4g"

def final DEFAULTS = [
        repository : "drools",
        mailRecip : "jporter@redhat.com"
]

def final REPO_CONFIGS = [
        "drools"      : [],
        "jbpm"        : [
                repository : "jbpm"
        ],
        "optaplanner" : [
                mailRecip:  DEFAULTS["mailRecip"] + ",gdsmet@redhat.com",
                repository: "optaplanner"
        ],
        "kiegroup"    : [
                repository: "kiegroup"
        ]
]

for (reps in REPO_CONFIGS) {
    Closure<Object> get = { String key -> reps.value[key] ?: DEFAULTS[key] }

    String repo = reps.key
    String mailRecip = get("mailRecip")

    def awp = """pipeline {
        agent {
            label "$AGENT_LABEL"
        }
        tools {
            maven "$mvnVersion"
            jdk "$javadk"
        }
        stages {
            stage('CleanWorkspace') {
                steps {
                    cleanWs()
                }
            }
            stage ('checkout website') {
                steps {
                    checkout([\$class: 'GitSCM', branches: [[name: $mainBranch ]], browser: [\$class: 'GithubWeb', repoUrl: 'https://github.com/kiegroup/${repo}-website'], doGenerateSubmoduleConfigurations: false, extensions: [[\$class: 'RelativeTargetDirectory', relativeTargetDir: '${repo}-website']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'https://github.com/kiegroup/${repo}-website']]])
                    dir("\${WORKSPACE}" + '/${repo}-website') {
                        sh '''pwd  
                           ls -al
                           git branch
                           '''
                           
                    }
                }
            }
            stage('Build website') {
                steps {
                    dir("${WORKSPACE}" + '/${repo}-website') {
                        sh 'mvn clean generate-resources'   
                    }
                }
            }
            stage('Publish website to filemgmt.jboss.org') {
                steps {
                    dir("${WORKSPACE}" + '/${repo}-website') {
                        sshagent(["${repo}-filemgmt"]) {
                            sh './build/rsync_website.sh'
                        }    
                    }
                }
            }              
        }
        post {
            failure{
                emailext body: 'Build log: \${BUILD_URL}consoleText\\n' +
                '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you do not have access to RedHat VPN please download and decompress attached file.)',
                subject: 'Build #\${BUILD_NUMBER} of ${repo}-web FAILED',
                to: '${mailRecip}'
                cleanWs()          
            }
            fixed {
                emailext body: '',
                subject: 'Build #\${BUILD_NUMBER} of ${repo}-web is fixed and was SUCCESSFUL',
                to: '${mailRecip}'
                cleanWs()     
            }
            success {
            cleanWs()
            }                    
        }
    }
"""
    pipelineJob("${folderPath}/${repo}-automatic-web-publishing") {

        description("this is a pipeline job for publishing automatically ${repo}-website")

        parameters {
            wHideParameterDefinition {
                name('AGENT_LABEL')
                defaultValue("${AGENT_LABEL}")
                description('name of machine where to run this job')
            }
            wHideParameterDefinition {
                name('mvnVersion')
                defaultValue("${mvnVersion}")
                description('version of maven')
            }
            wHideParameterDefinition {
                name('javadk')
                defaultValue("${javadk}")
                description('version of jdk')
            }
        }

        logRotator {
            numToKeep(3)
        }

        properties {
            githubProjectUrl("https://github.com/kiegroup/${repo}-website")
            pipelineTriggers {
                triggers {
                    githubPush()
                }
            }
        }

        definition {
            cps {
                script("${awp}")
                sandbox()
            }
        }

    }

}