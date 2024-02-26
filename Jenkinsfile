#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(
  // Container agents start faster and are easier to administer
  useContainerAgent: true,
  // Test Java 21 with default Jenkins version
  configurations: [
    [platform: 'linux',   jdk: '21'],
    [platform: 'windows', jdk: '21']
  ]
)
