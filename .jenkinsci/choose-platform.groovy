#!/usr/bin/env groovy

def choosePlatform() {
	if (params.Merge_PR) {
		return 'x86_64_aws_cov'
	}
	if (params.Linux) {
    return 'x86_64_aws_cov'
  }
  if (!params.Linux && params.MacOS) {
    return 'mac'
  }
  if (!params.Linux && !params.MacOS && params.ARMv8) {
    return 'armv8'
  }
  else if (!params.Linux && !params.MacOS && !params.ARMv8) {
    return 'armv7'
  }
}

return this
