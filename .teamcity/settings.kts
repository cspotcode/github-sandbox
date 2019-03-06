import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.dockerCompose
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_2.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2018.2"

project {

    vcsRoot(CspotcodeGithubSandbox2)

    buildType(BuildDeploy)
    buildType(Sandbox2Build)
    buildType(SandboxBuild)

    template(TemplateFoo)

    features {
        feature {
            id = "PROJECT_EXT_41"
            type = "OAuthProvider"
            param("clientId", "5ca0a478779220d067cc")
            param("secure:clientSecret", "credentialsJSON:6296df94-7eab-4b56-9bb8-95c44c46705e")
            param("displayName", "GitHub.com")
            param("gitHubUrl", "https://github.com/")
            param("providerType", "GitHub")
        }
        feature {
            id = "PROJECT_EXT_42"
            type = "IssueTracker"
            param("secure:password", "")
            param("name", "cspotcode/outdent")
            param("pattern", """#(\d+)""")
            param("authType", "anonymous")
            param("repository", "https://github.com/cspotcode/outdent")
            param("type", "GithubIssues")
            param("secure:accessToken", "")
            param("username", "")
        }
        feature {
            id = "PROJECT_EXT_56"
            type = "ReportTab"
            param("startPage", "report.html")
            param("title", "Test Report")
            param("type", "BuildReportTab")
        }
        feature {
            id = "PROJECT_EXT_57"
            type = "ReportTab"
            param("buildTypeId", "ZzzAbradleySandbox_SandboxBuild")
            param("startPage", "report.html")
            param("revisionRuleName", "lastSuccessful")
            param("revisionRuleRevision", "latest.lastSuccessful")
            param("title", "Test Report")
            param("type", "ProjectReportTab")
        }
    }
}

object BuildDeploy : BuildType({
    name = "Deploy infrastructure"

    vcs {
        root(DslContext.settingsRoot)
        root(CspotcodeGithubSandbox2)

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Step:Deploy"
            scriptContent = """echo "This is deploy""""
            dockerImage = "circleci/node:latest"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerPull = true
        }
    }

    triggers {
        vcs {
            triggerRules = """-:comment=.*\[ci test-only\].*:**"""
            branchFilter = ""
        }
    }

    features {
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:1dba5207-ef85-4983-912b-a49dab3563c2"
                }
            }
            param("github_oauth_user", "cspotcode")
        }
    }

    dependencies {
        snapshot(Sandbox2Build) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(SandboxBuild) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "deploy-artifact"
            }
        }
    }
})

object Sandbox2Build : BuildType({
    name = "sandbox2:Build"

    vcs {
        root(CspotcodeGithubSandbox2)
    }

    steps {
        script {
            name = "Build"
            scriptContent = """
                set -eux
                echo ${'$'}DOCKER_HOST
                bash ./.ci/build.sh
            """.trimIndent()
            dockerImage = "circleci/node:latest"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }

    triggers {
        vcs {
            branchFilter = ""
            enableQueueOptimization = false
        }
    }

    features {
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:1dba5207-ef85-4983-912b-a49dab3563c2"
                }
            }
            param("github_oauth_user", "cspotcode")
        }
    }
})

object SandboxBuild : BuildType({
    name = "sandbox:Build"

    artifactRules = """
        deploy-artifact
        report.html
    """.trimIndent()

    vcs {
        root(DslContext.settingsRoot)

        cleanCheckout = true
    }

    steps {
        script {
            name = "Step:Test"
            scriptContent = """
                echo "This is tests"
                if { git show HEAD | grep '\[ci test-only\]' ; } ; then
                    echo "skipping deploy because of commit message"
                else
                    echo "triggering deploy"
                    echo "trigger deploy" > deploy-artifact
                fi
                
                echo "<i>This is a report!</i>" > report.html
            """.trimIndent()
            dockerImage = "circleci/node:latest"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }

    triggers {
        vcs {
            branchFilter = ""
            enableQueueOptimization = false
        }
    }

    features {
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:1dba5207-ef85-4983-912b-a49dab3563c2"
                }
            }
            param("github_oauth_user", "cspotcode")
        }
    }

    requirements {
        equals("alive-agent", "true", "RQ_256")
    }
    
    disableSettings("RQ_256")
})

object TemplateFoo : Template({
    name = "templateFoo"

    steps {
        dockerCompose {
            name = "docker-compose"
            id = "RUNNER_1786"
        }
    }
})

object CspotcodeGithubSandbox2 : GitVcsRoot({
    name = "cspotcode/github-sandbox2"
    url = "https://github.com/cspotcode/github-sandbox2"
    authMethod = password {
        userName = "cspotcode"
    }
})
