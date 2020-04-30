/**
 * Creates pullrequest (PR) jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 90,
        ghAuthTokenId          : "kie-ci2-token",
        label                  : "kie-rhel7 && kie-mem8g",
        artifactsToArchive     : [
                "**/target/*.log",
                "**/target/testStatusListener*"
        ],
        excludedArtifacts      : [
                "**/target/checkstyle.log"
        ]
]

// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "lienzo-core"               : [
                timeoutMins: 30,
                label: "kie-rhel7 && kie-mem4g"
        ],
        "lienzo-tests"              : [
                timeoutMins: 30,
                label: "kie-rhel7 && kie-mem4g"
        ],
        "droolsjbpm-build-bootstrap": [
                timeoutMins: 30,
                label      : "kie-rhel7 && kie-mem4g"
        ],
        "kie-soup"                  : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "appformer"                 : [
                label    : "kie-rhel7 && kie-mem16g"
        ],
        "droolsjbpm-knowledge"      : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "drools"                    : [],
        "optaplanner"               : [],
        "optaweb-employee-rostering" : [
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/cypress/screenshots/**",
                        "**/cypress/videos/**"
                ]
        ],
        "optaweb-vehicle-routing" : [
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/cypress/screenshots/**",
                        "**/cypress/videos/**"
                ]
        ],
        "jbpm"                      : [
                timeoutMins: 120
        ],
        "kie-jpmml-integration"     : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "droolsjbpm-integration"    : [
                timeoutMins: 180,
                label    : "kie-rhel7 && kie-mem24g", // once https://github.com/kiegroup/kie-jenkins-scripts/pull/652 is reverted it will switch back to 16GB
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/kie-server-*ee7.war",
                        "**/target/kie-server-*webc.war",
                        "**/gclog" // this is a temporary file used to do some analysis: Once https://github.com/kiegroup/kie-jenkins-scripts/pull/652 is reverted this will disappear
                ]
        ],
        "openshift-drools-hacep"    : [],
        "droolsjbpm-tools"          : [],
        "kie-uberfire-extensions"   : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "kie-wb-playground"         : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "kie-wb-common"             : [
                timeoutMins: 120,
                label: "kie-rhel7 && kie-mem16g && gui-testing",
        ],
        "drools-wb"                 : [
                label: "kie-rhel7 && kie-mem16g"
        ],
        "optaplanner-wb"            : [],
        "jbpm-designer"             : [
                label: "kie-rhel7 && kie-mem16g"
        ],
        "jbpm-work-items"           : [
                label      : "kie-linux && kie-mem4g",
                timeoutMins: 30,
        ],
        "jbpm-wb"                   : [
                label: "kie-rhel7 && kie-mem16g",
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/jbpm-wb-case-mgmt-showcase*.war",
                        "**/target/jbpm-wb-showcase.war"
                ]
        ],
        "kie-wb-distributions"      : [
                label             : "kie-linux && kie-mem24g && gui-testing",
                timeoutMins       : 120,
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/target/screenshots/**",
                        "**/target/business-central*wildfly*.war",
                        "**/target/business-central*eap*.war",
                        "**/target/jbpm-server*dist*.zip"
                ]
        ]

]

def prJobs = '''
echo "Hello World"
'''

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")
    String ghAuthTokenId = get("ghAuthTokenId")

    // Creation of folders where jobs are stored
    folder(Constants.PULL_REQUEST_FOLDER)


    // jobs for master branch don't use the branch in the name
    String jobName = Constants.PULL_REQUEST_FOLDER + "/$repo-$repoBranch" + ".pullrequests"
    pipelineJob(jobName) {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        disabled()

        logRotator {
            numToKeep(5)
            daysToKeep(5)
        }

        properties {
            githubProjectUrl("https://github.com/mbiarnes/${repo}/")
        }

        parameters {
            stringParam {
                name("sha1")
                defaultValue("")
                description("")
                trim(false)
            }
            stringParam {
                name("ADDITIONAL_ARTIFACTS_TO_ARCHIVE")
                defaultValue("${get('artifactsToArchive')}")
                description('')
                trim(false)
            }
        }

        definition {
            cpsScm {
                scm {
                    gitSCM {
                        userRemoteConfigs {
                            userRemoteConfig {
                                url("https://github.com/mbiarnes/${repo}/")
                                credentialsId("kie-ci")
                                name("")
                                refspec("")
                            }
                        }
                        branches {
                            branchSpec {
                                name("mbiarnes_PR_tests")
                            }
                        }
                        browser {
                            githubWeb{
                                repoUrl("https://github.com/mbiarnes/${repo}/")
                            }
                        }
                        doGenerateSubmoduleConfigurations(false)
                        gitTool("")
                    }
                }
                scriptPath("Jenkinsfile")
            }
        }

        triggers {
            ghprbTrigger {
                orgslist("")
                onlyTriggerPhrase(false)
                gitHubAuthId(get("ghAuthTokenId"))
                adminlist("mbiarnes kiereleaseuser")
                whitelist("kiegroup")
                cron("H/5 * * * *")
                triggerPhrase(".*\\[skip\\W+ci\\].*")
                allowMembersOfWhitelistedOrgsAsAdmin(true)
                whiteListTargetBranches {
                    ghprbBranch {
                        branch("mbiarnes_PR_tests")
                    }
                }
                useGitHubHooks(false)
                permitAll(false)
                autoCloseFailedPullRequests(false)
                displayBuildErrorsOnDownstreamBuilds(false)
                blackListCommitAuthor("")
                commentFilePath("")
                skipBuildPhrase("this has to be filled and should not be empty")
                msgSuccess("Success")
                msgFailure("Failure")
                commitStatusContext("")
                buildDescTemplate("")
                blackListLabels("")
                whiteListLabels("")
                extensions {
                    ghprbSimpleStatus {
                        commitStatusContext("Linux-Pull Request")
                        addTestResults(true)
                        showMatrixStatus(false)
                        statusUrl("")
                        triggeredStatus("")
                        startedStatus("")
                    }
                }
                includedRegions("")
                excludedRegions("")
            }
        }
    }
}
