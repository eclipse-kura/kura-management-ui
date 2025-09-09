@Library('add-ons-shared-libs@develop') _

node {
    continuousIntegrationPipeline(
        buildType: "deploy",
        sonar: [
            enable: true,
            projectKey: "eclipse-kura_kura-management-ui",
            tokenId: "sonarcloud-token-kura-management-ui",
            exclusions: "tests/**/*.java"
        ],
    )
}
