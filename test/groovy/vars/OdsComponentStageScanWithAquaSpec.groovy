package vars

import org.ods.component.Context
import org.ods.component.IContext
import org.ods.component.ScanWithAquaStage
import org.ods.services.AquaService
import org.ods.services.BitbucketService
import org.ods.services.OpenShiftService
import org.ods.services.ServiceRegistry
import org.ods.util.Logger
import spock.lang.Shared
import vars.test_helper.PipelineSpockTestBase

class OdsComponentStageScanWithAquaSpec extends PipelineSpockTestBase {
    private Logger logger = Mock(Logger)

    @Shared
    def config = [
        bitbucketUrl: 'https://bitbucket.example.com',
        projectId: 'foo',
        componentId: 'bar',
        repoName: 'foo-bar',
        gitUrl: 'https://bitbucket.example.com/scm/foo/foo-bar.git',
        gitCommit: 'cd3e9082d7466942e1de86902bb9e663751dae8e',
        gitCommitMessage: """Foo\n\nSome "explanation".""",
        gitCommitAuthor: "John O'Hare",
        gitCommitTime: '2020-03-23 12:27:08 +0100',
        gitBranch: 'master',
        buildUrl: 'https://jenkins.example.com/job/foo-cd/job/foo-cd-bar-master/11/console',
        buildTime: '2020-03-23 12:27:08 +0100',
        odsSharedLibVersion: '2.x',
        branchToEnvironmentMapping: ['master': 'dev', 'release/': 'test'],
    ]

    def "run successfully"() {
        given:
        def c = config + [environment: 'dev']
        IContext context = new Context(null, c, logger)
        context.addBuildToArtifactURIs("bar", [image: "image1/image1:2323232323"])

        AquaService aquaService = Stub(AquaService.class)
        aquaService.scanViaCli(*_) >> 0
        ServiceRegistry.instance.add(AquaService, aquaService)

        BitbucketService bitbucketService = Stub(BitbucketService.class)
        bitbucketService.createCodeInsightReport(*_) >> null
        ServiceRegistry.instance.add(BitbucketService, bitbucketService)

        OpenShiftService openShiftService = Stub(OpenShiftService.class)
        openShiftService.getConfigMapData(ScanWithAquaStage.AQUA_GENERAL_CONFIG_MAP_PROJECT,
            ScanWithAquaStage.AQUA_CONFIG_MAP_NAME) >> [
            enabled: true,
            alertEmails: "mail1@mail.com",
            url: "http://aqua",
            registry: "internal"
        ]
        openShiftService.getConfigMapData("foo", ScanWithAquaStage.AQUA_CONFIG_MAP_NAME) >> [
            enabled: true
        ]
        ServiceRegistry.instance.add(OpenShiftService, openShiftService)

        when:
        def script = loadScript('vars/odsComponentStageScanWithAqua.groovy')
        helper.registerAllowedMethod('readFile', [ Map ]) { Map args -> }
        helper.registerAllowedMethod('readJSON', [ Map ]) { [
            vulnerability_summary: [critical: 0, malware: 0]
        ] }
        helper.registerAllowedMethod('sh', [ Map ]) { Map args -> }
        helper.registerAllowedMethod('archiveArtifacts', [ Map ]) { Map args -> }
        helper.registerAllowedMethod('stash', [ Map ]) { Map args -> }
        script.call(context)

        then:
        printCallStack()
        assertJobStatusSuccess()
    }
}
