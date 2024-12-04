package rundeck.services

import com.dtolabs.rundeck.core.VersionConstants
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypterPlugin
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.audit.AuditEventListenerPlugin
import com.dtolabs.rundeck.plugins.config.PluginGroup
import com.dtolabs.rundeck.plugins.file.FileUploadPlugin
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin
import com.dtolabs.rundeck.plugins.user.groups.UserGroupSourcePlugin
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import com.dtolabs.rundeck.server.plugins.services.StorageConverterPluginProviderService
import com.dtolabs.rundeck.core.storage.service.StoragePluginProviderService
import com.dtolabs.rundeck.server.plugins.services.UIPluginProviderService
import grails.core.GrailsApplication
import grails.web.mapping.LinkGenerator
import org.grails.web.util.WebUtils
import org.springframework.context.NoSuchMessageException
import org.springframework.web.context.request.RequestContextHolder
import rundeck.services.feature.FeatureService

import javax.servlet.ServletContext

class PluginApiService {

    ServletContext servletContext
    GrailsApplication grailsApplication
    FrameworkService frameworkService
    def messageSource
    UiPluginService uiPluginService
    UIPluginProviderService uiPluginProviderService
    NotificationService notificationService
    LoggingService loggingService
    PluginService pluginService
    ScmService scmService
    LogFileStorageService logFileStorageService
    StoragePluginProviderService storagePluginProviderService
    StorageConverterPluginProviderService storageConverterPluginProviderService
    FeatureService featureService
    ExecutionLifecycleComponentService executionLifecycleComponentService
    JobLifecycleComponentService jobLifecycleComponentService
    def rundeckPluginRegistry
    LinkGenerator grailsLinkGenerator

    def listPluginsDetailed() {
        //list plugins and config settings for project/framework props
        IFramework framework = frameworkService.getRundeckFramework()
        Locale locale = getLocale()

        Map<String, List<Description>> pluginDescs = [:]

        StepExecutionService ses = framework.getStepExecutionService()
        pluginDescs[ses.name] = pluginService.listPlugins(StepExecutor, ses)
                .findAll { it.value.description != null }
                .collect { it.value.description }
                .sort {a,b -> a.name <=> b.name }

        NodeExecutorService nes = framework.getNodeExecutorService()
        pluginDescs[nes.name] = pluginService.listPlugins(NodeExecutor, nes)
                .collect { it.value.description }
                .sort { a, b -> a.name <=> b.name }

        NodeStepExecutionService nses = framework.getNodeStepExecutorService()
        pluginDescs[nses.name] = pluginService.listPlugins(NodeStepExecutor, nses)
                .collect { it.value.description }
                .sort { a, b -> a.name <=> b.name }

        FileCopierService fcs = framework.getFileCopierService()
        pluginDescs[fcs.name] = pluginService.listPlugins(FileCopier, fcs)
                .collect { it.value.description }
                .sort { a, b -> a.name <=> b.name }

        ResourceModelSourceService rmss = framework.getResourceModelSourceService()
        pluginDescs[rmss.name] = pluginService.listPlugins(ResourceModelSourceFactory, rmss)
                .findAll { it.value.description != null }
                .collect { it.value.description }
                .sort { a, b -> a.name <=> b.name }

        pluginDescs[ServiceNameConstants.NodeEnhancer]=pluginService.listPlugins(NodeEnhancerPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }

        ResourceFormatParserService rfps = framework.getResourceFormatParserService()
        pluginDescs[rfps.name] = pluginService.listPlugins(ResourceFormatParser, rfps)
                .collect { it.value.description }
                .sort { a, b -> a.name <=> b.name }

        ResourceFormatGeneratorService rfgs = framework.getResourceFormatGeneratorService()
        pluginDescs[rfgs.name] = pluginService.listPlugins(ResourceFormatGenerator, rfgs)
                .collect { it.value.description }
                .sort { a, b -> a.name <=> b.name }

        OrchestratorService os = framework.getOrchestratorService()
        pluginDescs[os.name] = pluginService.listPlugins(OrchestratorPlugin, os)
                .collect { it.value.description }
                .sort { a, b -> a.name <=> b.name }

        if(featureService.featurePresent(Features.OPTION_VALUES_PLUGIN)) {
            pluginDescs['OptionValues'] = pluginService.listPlugins(OptionValuesPlugin).collect {
                it.value.description
            }.sort { a, b -> a.name <=> b.name }
        }

        pluginDescs['PasswordUtilityEncrypter'] = pluginService.listPlugins(PasswordUtilityEncrypterPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }

        //web-app level plugin descriptions
        if(featureService.featurePresent(Features.JOB_LIFECYCLE_PLUGIN)) {
            pluginDescs[jobLifecycleComponentService.jobLifecyclePluginProviderService.name]=jobLifecycleComponentService.listJobLifecyclePlugins().collect {
                it.value.description
            }.sort { a, b -> a.name <=> b.name }
        }
        if(featureService.featurePresent(Features.EXECUTION_LIFECYCLE_PLUGIN)) {
            pluginDescs[executionLifecycleComponentService.executionLifecyclePluginProviderService.name]=executionLifecycleComponentService.listExecutionLifecyclePlugins().collect {
                it.value.description
            }.sort { a, b -> a.name <=> b.name }
        }
        pluginDescs[notificationService.notificationPluginProviderService.name]=notificationService.listNotificationPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[loggingService.streamingLogReaderPluginProviderService.name]=loggingService.listStreamingReaderPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[loggingService.streamingLogWriterPluginProviderService.name]=loggingService.listStreamingWriterPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[logFileStorageService.executionFileStoragePluginProviderService.name]= logFileStorageService.listLogFileStoragePlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[storagePluginProviderService.name]= pluginService.listPlugins(StoragePlugin.class, storagePluginProviderService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[storageConverterPluginProviderService.name] = pluginService.listPlugins(StorageConverterPlugin.class, storageConverterPluginProviderService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[scmService.scmExportPluginProviderService.name]=scmService.listPlugins('export').collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[scmService.scmImportPluginProviderService.name]=scmService.listPlugins('import').collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }

        if(featureService.featurePresent(Features.FILE_UPLOAD_PLUGIN)) {
            pluginDescs['FileUpload'] = pluginService.listPlugins(FileUploadPlugin).collect {
                it.value.description
            }.sort { a, b -> a.name <=> b.name }
        }
        pluginDescs['LogFilter'] = pluginService.listPlugins(LogFilterPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['ContentConverter']=pluginService.listPlugins(ContentConverterPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['TourLoader']=pluginService.listPlugins(TourLoaderPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['UserGroupSource']=pluginService.listPlugins(UserGroupSourcePlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['UI']= pluginService.listPlugins(UIPlugin, uiPluginProviderService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['WebhookEvent']=pluginService.listPlugins(WebhookEventPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[ServiceNameConstants.AuditEventListener] = pluginService.listPlugins(AuditEventListenerPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[ServiceNameConstants.PluginGroup] = pluginService.listPlugins(PluginGroup).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }


        Map<String,Map> uiPluginProfiles = [:]
        def loadedFileNameMap=[:]
        pluginDescs.each { svc, list ->
            if(svc == "UI") return
            list.each { desc ->
                def provIdent = svc + ":" + desc.name
                uiPluginProfiles[provIdent] = uiPluginService.getProfileFor(svc, desc.name)
                def filename = uiPluginProfiles[provIdent].get('fileMetadata')?.filename
                if(filename){
                    if(!loadedFileNameMap[filename]){
                        loadedFileNameMap[filename]=[]
                    }
                    loadedFileNameMap[filename]<< provIdent
                }
            }
        }

        def defaultScopes=[
                (framework.getNodeStepExecutorService().name) : PropertyScope.InstanceOnly,
                (framework.getStepExecutionService().name) : PropertyScope.InstanceOnly,
        ]
        def bundledPlugins=[
                (framework.getNodeExecutorService().name): framework.getNodeExecutorService().getBundledProviderNames(),
                (framework.getFileCopierService().name): framework.getFileCopierService().getBundledProviderNames(),
                (framework.getResourceFormatParserService().name): framework.getResourceFormatParserService().getBundledProviderNames(),
                (framework.getResourceFormatGeneratorService().name): framework.getResourceFormatGeneratorService().getBundledProviderNames(),
                (framework.getResourceModelSourceService().name): framework.getResourceModelSourceService().getBundledProviderNames(),
                (storagePluginProviderService.name): storagePluginProviderService.getBundledProviderNames()+['db'],
                FileUpload: ['filesystem-temp'],
        ]
        //list included plugins
        def embeddedList = frameworkService.listEmbeddedPlugins(grailsApplication)
        def embeddedFilenames=[]
        if(embeddedList.success && embeddedList.pluginList){
            embeddedFilenames=embeddedList.pluginList*.fileName
        }
        def specialConfiguration=[
                (storagePluginProviderService.name):[
                        description: message("plugin.storage.provider.special.description",locale),
                        prefix:"rundeck.storage.provider.[index].config."
                ],
                (storageConverterPluginProviderService.name):[
                        description: message("plugin.storage.converter.special.description",locale),
                        prefix:"rundeck.storage.converter.[index].config."
                ],
                (framework.getResourceModelSourceService().name):[
                        description: message("plugin.resourceModelSource.special.description",locale),
                        prefix:"resources.source.[index].config."
                ],
                (logFileStorageService.executionFileStoragePluginProviderService.name):[
                        description: message("plugin.executionFileStorage.special.description",locale),
                ],
                (scmService.scmExportPluginProviderService.name):[
                        description: message("plugin.scmExport.special.description",locale),
                ],
                (scmService.scmImportPluginProviderService.name):[
                        description: message("plugin.scmImport.special.description",locale),
                ],
                FileUpload:[
                        description: message("plugin.FileUpload.special.description",locale),
                ]
        ]
        def specialScoping=[
                (scmService.scmExportPluginProviderService.name):true,
                (scmService.scmImportPluginProviderService.name):true
        ]

        [
                descriptions        : pluginDescs,
                serviceDefaultScopes: defaultScopes,
                bundledPlugins      : bundledPlugins,
                embeddedFilenames   : embeddedFilenames,
                specialConfiguration: specialConfiguration,
                specialScoping      : specialScoping,
                uiPluginProfiles    : uiPluginProfiles
        ]
    }

    def listPlugins() {
        Locale locale = getLocale()
        long appEpoch = VersionConstants.DATE.time
        String appVer = VersionConstants.VERSION
        def pluginList = listPluginsDetailed()
        def tersePluginList = pluginList.descriptions.collect {
            String service = it.key
            def providers = it.value.collect { provider ->
                def meta = rundeckPluginRegistry.getPluginMetadata(service, provider.name)
                boolean builtin = meta == null
                String ver = meta?.pluginFileVersion ?: appVer
                String artifactName = meta?.pluginArtifactName ?: provider.name
                String tgtHost = meta?.targetHostCompatibility ?: 'all'
                String rdVer = meta?.rundeckCompatibilityVersion ?: 'unspecified'
                String author = meta?.pluginAuthor ?: ''
                String id = meta?.pluginId ?: PluginUtils.generateShaIdFromName(artifactName)

                def pluginDesc = [
                 pluginId   : id,
                 pluginName : artifactName,
                 name         : provider.name,
                 title        : provider.title,
                 description  : provider.description,
                 isHighlighted  : provider.isHighlighted(),
                 highlightedOrder      : provider.getOrder(),
                 builtin      : builtin,
                 pluginVersion: ver,
                 pluginAuthor : author,
                 rundeckCompatibilityVersion: rdVer,
                 targetHostCompatibility: tgtHost,
                 pluginDate   : meta?.pluginDate?.time?:appEpoch,
                 enabled      : true] as LinkedHashMap<Object, Object>

                if(service != ServiceNameConstants.UI) {
                    def uiDesc = uiPluginService.getProfileFor(service, provider.name)
                    if (uiDesc.icon)
                        pluginDesc.iconUrl = grailsLinkGenerator.link(
                                controller: 'plugin',
                                action: 'pluginIcon',
                                params: [service: service, name: provider.name],
                                absolute: true)

                    if (uiDesc.providerMetadata) {
                        pluginDesc.providerMetadata = uiDesc.providerMetadata
                    }
                }

                return pluginDesc
            }
            [service  : it.key,
             desc     : message("framework.service.${service}.description".toString(),locale),
             providers: providers
            ]
        }
        tersePluginList
    }

    /**
     *
     * @param property property
     * @return true if the property has a feature flag requirement and the feature is present, or no requirement is specified
     */
    boolean hasRequiredFeatures(Property property){
        if (property.renderingOptions.containsKey(StringRenderingConstants.FEATURE_FLAG_REQUIRED)) {
            def featureFlag = property.renderingOptions[StringRenderingConstants.FEATURE_FLAG_REQUIRED]?.toString()
            if (featureFlag && !featureService.featurePresent(featureFlag)) {
                return false
            }
        }
        return true
    }

    List<Map> pluginPropertiesAsMap(String service, String pluginName, List<Property> properties) {
        properties.findAll {
            hasRequiredFeatures(it)
        }.collect { Property prop ->
            pluginPropertyMap(service, pluginName, prop)
        }
    }

    /**
     * Return map representation of plugin property definition
     * @param service service
     * @param pluginName provider
     * @param prop property
     * @param locale locale
     * @return
     */
    Map pluginPropertyMap(String service, String pluginName, Property prop) {
        def optsMap = asStringMap(prop)
        def staticTextDefaultValue = uiPluginService.getPluginMessage(
            service,
            pluginName,
            "property.${prop.name}.defaultValue",
            prop.defaultValue ?: '',
            locale
        )
        if(staticTextDefaultValue && optsMap && optsMap['staticTextContentType'] in ['text/html','text/markdown']){
            if(optsMap['staticTextContentType'] == 'text/markdown') {
                staticTextDefaultValue = staticTextDefaultValue.decodeMarkdown()
                optsMap['staticTextContentType'] = 'application/x-text-html-sanitized'
            }else{
                staticTextDefaultValue = staticTextDefaultValue.encodeAsSanitizedHTML()
                optsMap['staticTextContentType'] = 'application/x-text-html-sanitized'
            }
        }
        [
            name                  : prop.name,
            desc                  : uiPluginService.getPluginMessage(
                service,
                pluginName,
                "property.${prop.name}.description",
                prop.description ?: '',
                locale
            ),
            title                 : uiPluginService.getPluginMessage(
                service,
                pluginName,
                "property.${prop.name}.title",
                prop.title ?: prop.name,
                locale
            ),
            defaultValue          : prop.defaultValue,
            staticTextDefaultValue: staticTextDefaultValue,
            required              : prop.required,
            type                  : prop.type.toString(),
            allowed               : prop.selectValues,
            selectLabels          : prop.selectLabels,
            scope                 : prop.scope?.toString(),
            options               : optsMap
        ]
    }

    public Map<String, String> asStringMap(Property prop) {
        if (!prop.renderingOptions) {
            return null
        }
        Map<String, String> opts = [:]
        prop.renderingOptions.each { String k, Object v ->
            opts[k]=v.toString()
        }
        opts
    }

    def listInstalledPluginIds() {
        def idList = [:]
        def plugins = pluginService.listPlugins(UIPlugin, uiPluginProviderService)
        plugins.each{ entry ->
            def meta = rundeckPluginRegistry.getPluginMetadata("UI", entry.key)
            String id = meta?.pluginId ?: PluginUtils.generateShaIdFromName(entry.key)
            idList[id] = meta?.pluginFileVersion ?: "1.0.0"
        }

        listPlugins().each { p ->
            p.providers.each { idList[it.pluginId] = it.pluginVersion }
        }

        return idList
    }

    Locale getLocale() {
        RequestContextHolder.getRequestAttributes() ? WebUtils.retrieveGrailsWebRequest().getLocale() : Locale.default
    }

    private def message(String code, Locale locale) {
        try {
            messageSource.getMessage(code,[].toArray(),locale)
        } catch(NoSuchMessageException nsme) {
            return code
        }

    }

}
