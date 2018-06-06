#!/usr/bin/env groovy

enum CoveragePlatforms {
  x86_64_aws_cov, mac, armv8, armv7
}

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

def setParallelism(defaultParameter) {
  if (!defaultParameter) {
    return 4
  }
  if (env.NODE_NAME.contains('arm7')) {
    return 1
  }
  if (env.NODE_NAME.contains('mac')) {
    return 4
  }
  if (env.NODE_NAME.contains('x86_64')) {
    return 8
  }
  return defaultParameter
}

return this
