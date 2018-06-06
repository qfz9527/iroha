#!/usr/bin/env groovy

// upload artifacts in release builds
def postStep() {
  def artifacts = load ".jenkinsci/artifacts.groovy"
  def commit = env.GIT_COMMIT
  def platform = sh(script: 'uname -m', returnStdout: true).trim()
  filePaths = [ '/tmp/${GIT_COMMIT}-${BUILD_NUMBER}/*' ]
  artifacts.uploadArtifacts(filePaths, sprintf('/iroha/linux/%4$s/%1$s-%2$s-%3$s', [GIT_LOCAL_BRANCH, sh(script: 'date "+%Y%m%d"', returnStdout: true).trim(), commit.substring(0,6), platform]))
}

// upload artifacts in release builds (for mac)
def macPostStep() {
  def artifacts = load ".jenkinsci/artifacts.groovy"
  def commit = env.GIT_COMMIT
  filePaths = [ '\$(pwd)/build/*.tar.gz' ]
  artifacts.uploadArtifacts(filePaths, sprintf('/iroha/macos/%1$s-%2$s-%3$s', [GIT_LOCAL_BRANCH, sh(script: 'date "+%Y%m%d"', returnStdout: true).trim(), commit.substring(0,6)]))
}

// clean docker containers after the stage
def cleanUp() {
  if ( ! env.NODE_NAME.contains('x86_64') ) {
    def cleanup = load ".jenkinsci/docker-cleanup.groovy"
    cleanup.doDockerCleanup()
  }
}

// stop postgres and remove workspace folder (for mac)
def macCleanUp() {
  sh """
    pg_ctl -D /var/jenkins/${GIT_COMMIT}-${BUILD_NUMBER}/ stop && \
    rm -rf /var/jenkins/${GIT_COMMIT}-${BUILD_NUMBER}/
  """
}

return this
