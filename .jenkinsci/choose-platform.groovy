#!/usr/bin/env groovy

def choosePlatform() {
  if (params.Merge_PR) {
		return CoveragePlatforms.x86_64_aws_cov.toString()
	}
	if (params.Linux) {
    return CoveragePlatforms.x86_64_aws_cov.toString()
  }
  if (!params.Linux && params.MacOS) {
    return CoveragePlatforms.mac.toString()
  }
  if (!params.Linux && !params.MacOS && params.ARMv8) {
    return CoveragePlatforms.armv8.toString()
  }
  else if (!params.Linux && !params.MacOS && !params.ARMv8) {
    return CoveragePlatforms.armv7.toString()
  }
}

return this
