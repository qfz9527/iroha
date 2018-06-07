#!/usr/bin/env groovy

// remove docker network and stale images
def doDockerCleanup() {
  sh """
    # Check whether the image is the last-standing man
    # i.e., no other tags exist for this image
    docker rmi \$(docker images --no-trunc --format '{{.Repository}}:{{.Tag}}\\t{{.ID}}' | grep \$(docker images --no-trunc --format '{{.ID}}' ${iC.id}) | head -n -1 | cut -f 1) || true
    sleep 5
    docker network rm $IROHA_NETWORK || true
  """
}

return this
