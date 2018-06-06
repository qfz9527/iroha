#!/usr/bin/env groovy

enum GithubPRStatus {
	APPROVED, CHANGES_REQUESTED
}
enum MergeTarget {
	merge, squash
}
enum ChangeTarget {
	master, develop, trunk
}

// merges pull request using GitHub API in case it meets the merging criteria
def mergePullRequest() {
	if ( ! ( checkMergeAcceptance() ) ) { return false  }
	withCredentials([string(credentialsId: 'jenkins-integration-test', variable: 'sorabot')]) {
		def slurper = new groovy.json.JsonSlurperClassic()
		def commitTitle = ""
		def commitMessage = ""
		def mergeMethod = getMergeMethod()
		def jsonResponseMerge = sh(script: """
		curl -H "Authorization: token ${sorabot}" \
				 -H "Accept: application/vnd.github.v3+json" \
				 -X PUT --data '{"commit_title":"${commitTitle}","commit_message":"${commitMessage}","sha":"${env.GIT_COMMIT}","merge_method":"${mergeMethod}"}' \
				 -w "%{http_code}" https://api.github.com/repos/hyperledger/iroha/pulls/${CHANGE_ID}/merge""", returnStdout: true)
		def githubResponse = sh(script:"""set +x; printf '%s\n' "${jsonResponseMerge}" | tail -n 1; set -x""", returnStdout: true).trim()
		jsonResponseMerge = slurper.parseText(jsonResponseMerge)
		if (jsonResponseMerge.merged != "true" || !(githubResponse ==~ "200")) {
			echo jsonResponseMerge.message
			return false
		}
		return true
	}
}

// check merge acceptance by:
// - at least 2 "approved and NO "changes_requested" in reviews
// - e-mail of the commit does not match Jenkins user who launched this build
def checkMergeAcceptance() {
	def approvalsRequired = 2
	def gitCommitterEmail = sh(script: 'git --no-pager show -s --format=\'%ae\'', returnStdout: true).trim()
	wrap([$class: 'BuildUser']) {
		jenkinsCommitterEmail = env.BUILD_USER_EMAIL
	}
  withCredentials([string(credentialsId: 'jenkins-integration-test', variable: 'sorabot')]) {
  	def slurper = new groovy.json.JsonSlurperClassic()
  	def jsonResponseReview = sh(script: """
  		curl -H "Authorization: token ${sorabot}" -H "Accept: application/vnd.github.v3+json" https://api.github.com/repos/hyperledger/iroha/pulls/${CHANGE_ID}/reviews
			""", returnStdout: true).trim()
		jsonResponseReview = slurper.parseText(jsonResponseReview)
		if (jsonResponseReview.size() > 0) {
			jsonResponseReview.each {
				if ("${it.state}" == GithubPRStatus.APPROVED.toString()) {
					approvalsRequired -= 1
				}
				if ("${it.state}" == GithubPRStatus.CHANGES_REQUESTED.toString()) {
					return false
				}
			}
		}
		if (approvalsRequired > 0) {
			sh "echo 'Merge failed. Get more PR approvals before merging'"
			return false
		}
		else if (gitCommitterEmail != jenkinsCommitterEmail) {
			sh "echo 'Merge failed. Email of the commit does not match Jenkins user'"
			return false
		}
		else {
			return true
		}
	}
}

// returns merge method based on target branch (squash&merge vs merge)
def getMergeMethod() {
	if (env.CHANGE_TARGET == ChangeTarget.master.toString()) {
		return MergeTarget.merge.toString()
	}
	else {
		return MergeTarget.squash.toString()
	}
}

// returns PR reviewers in the form of "@reviewer1 @reviewer2 ... @reviewerN" to mention PR reviewers about build result
def getPullRequestReviewers() {
	def slurper = new groovy.json.JsonSlurperClassic()
	def ghUsersList = ''
	// there is no github API that allows to get all reviewers for the PR. 
	// thus we use github API twice to request for reviews and requested reviewers. afterwards, results are concatenated with @
	def jsonResponseReviewers = sh(script: """
		curl https://api.github.com/repos/hyperledger/iroha/pulls/${CHANGE_ID}/requested_reviewers
		""", returnStdout: true).trim()
	def jsonResponseReview = sh(script: """
		curl https://api.github.com/repos/hyperledger/iroha/pulls/${CHANGE_ID}/reviews
		""", returnStdout: true).trim()
	jsonResponseReviewers = slurper.parseText(jsonResponseReviewers)
	if (jsonResponseReviewers.size() > 0) {
		jsonResponseReviewers.users.each {
			ghUsersList = ghUsersList.concat("@${it.login} ")
		}
	}
	jsonResponseReview = slurper.parseText(jsonResponseReview)
	if (jsonResponseReview.size() > 0) {
		jsonResponseReview.each {
			if ("${it.state}" == GithubPRStatus.APPROVED.toString() || "${it.state}" == GithubPRStatus.CHANGES_REQUESTED.toString()) {
				ghUsersList = ghUsersList.concat("@${it.user.login} ")
			}
		}
	}
	return ghUsersList
}

// post a comment on PR via GitHub API
def writePullRequestComment() {
	def ghUsersList = getPullRequestReviewers()
	withCredentials([string(credentialsId: 'jenkins-integration-test', variable: 'sorabot')]) {
		def slurper = new groovy.json.JsonSlurperClassic()
		def jsonResponseComment = sh(script: """
			curl -H "Authorization: token ${sorabot}" \
			-H "Accept: application/vnd.github.v3+json" \
			-X POST --data '{"body":"${ghUsersList} commit ${env.GIT_COMMIT} build status: ${currentBuild.currentResult}. build URL: ${BUILD_URL}"}' \
			-w "%{http_code}" https://api.github.com/repos/hyperledger/iroha/issues/${CHANGE_ID}/comments
			""", returnStdout: true).trim()		
		def githubResponse = sh(script:"""set +x; printf '%s\n' "${jsonResponseComment}" | tail -n 1 ; set -x""", returnStdout: true).trim()
		if (githubResponse ==~ "201") {
			return true
		}
	}
	return false
}

return this
