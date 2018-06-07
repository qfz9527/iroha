#!/usr/bin/env groovy

// format the enum elements output like "(val1|val2|...|valN)*"
def printRange(start, end) {
  def output = ""
  for (type in start..end) {
    output = [output, (type.name() != start.toString() ? "|" : ""), type.name()].join('')
  }
  return ["(", output, ")*"].join('')
}

// return tests list regex that will be launched by ctest
def chooseTestType() {
	if (params.Merge_PR) {
		if (env.NODE_NAME.contains('x86_64')) {
			// choose module, integration, system, cmake, regression tests
			return printRange(TestTypes.module, TestTypes.regression)
		}
		else {
			// not to do any tests
			return ""
		}
	}
	if (params.Nightly) {
		if (env.NODE_NAME.contains('x86_64')) {
			// choose all tests
			return printRange(TestTypes.MIN_VALUE, TestTypes.MAX_VALUE)
		}
		else {
			// choose module, integration, system, cmake, regression tests
			return printRange(TestTypes.module, TestTypes.regression)
		}
	}
	// just choose module tests
	return [TestTypes.module.toString(), "*"].join('')
}

return this
