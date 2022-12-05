#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(
  // Container agents start faster and are easier to administer
  useContainerAgent: true,
  // Test Java 11 with default Jenkins version
  configurations: [
    [platform: 'linux',   jdk: '11'],
    [platform: 'windows', jdk: '11']
  ]
)
