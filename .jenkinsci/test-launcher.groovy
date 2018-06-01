#!/usr/bin/env groovy

def chooseTestType() {
	if (params.Merge_PR) {
		if (env.NODE_NAME.contains('x86_64')) {
			return "(module|integration|system|cmake|regression)*"	
		}
		else {
			return ""
		}
	}
	if (params.Nightly) {
		if (env.NODE_NAME.contains('x86_64')) {
			return "*"
		}
		else {
			return "(module|integration|system|cmake|regression)*"
		}
	}
	return "module*"
}

return this
