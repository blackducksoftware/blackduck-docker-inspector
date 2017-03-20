package com.blackducksoftware.integration.hub.linux

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.linux.creator.Creator
import com.blackducksoftware.integration.hub.linux.extractor.Extractor
import com.blackducksoftware.integration.hub.util.HostnameHelper

@Component
class HubLinuxManager {
    private final Logger logger = LoggerFactory.getLogger(HubLinuxManager.class)

    @Value('${working.directory}')
    String workingDirectoryPath

    @Value('${command.timeout}')
    long commandTimeout

    @Autowired
    OperatingSystemFinder operatingSystemFinder

    @Autowired
    HubClient hubClient

    @Autowired
    BdioFileWriter bdioFileWriter

    @Autowired
    List<Creator> creators

    @Autowired
    List<Extractor> extractors

    void performExtractFromRemoteInspection() {
        def workingDirectory = new File(workingDirectoryPath)
        workingDirectory.mkdirs()

        def allExtractionResults = extractAllBdioComponentDetailsFromWorkingDirectory(workingDirectory)
        createAndUploadBdioFile(workingDirectory, allExtractionResults)
    }

    void performExtractFromLocalInspection() {
        OperatingSystemEnum operatingSystemEnum = operatingSystemFinder.determineOperatingSystem()
        String projectName = HostnameHelper.getMyHostname()
        String projectVersionName = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())

        logger.info("Operating System: {}",operatingSystemEnum.forge)
        logger.info("Project Name:     {}",projectName)
        logger.info("Project Version:  {}",projectVersionName)

        def workingDirectory = new File(workingDirectoryPath)
        workingDirectory.mkdirs()

        creators.each {
            logger.info "Starting file creation with ${it.getClass().name}"
            if (!it.isCommandAvailable(commandTimeout)) {
                logger.info("Can't create with ${it.getClass().name} - command is not available.")
            } else{
                String filename = it.filename(projectName, projectVersionName, operatingSystemEnum)
                File outputFile = new File(workingDirectory, filename)
                if(outputFile.exists()){
                    outputFile.delete()
                }
                it.writeOutputFile(outputFile, commandTimeout)
                logger.info("Created file ${outputFile.canonicalPath}")
            }
        }

        def allExtractionResults = extractAllBdioComponentDetailsFromWorkingDirectory(workingDirectory)
        createAndUploadBdioFile(workingDirectory, allExtractionResults)
    }

    private ExtractionResults extractAllBdioComponentDetailsFromWorkingDirectory(File workingDirectory) {
        def allExtractionResults = new ExtractionResults()
        workingDirectory.eachFile() { file ->
            logger.info("Processing file ${file.name}")
            extractors.each { extractor ->
                if (extractor.shouldAttemptExtract(file)) {
                    logger.info("Extracting ${file.name} with ${extractor.getClass().name}")
                    def extractionResult = extractor.extract(file)
                    allExtractionResults.addExtractionResult(extractionResult)
                }
            }
        }

        allExtractionResults
    }

    private createAndUploadBdioFile(File workingDirectory, ExtractionResults allExtractionResults) {
        def extractionResults = allExtractionResults.getExtractionResults()
        extractionResults.each { extractionResult ->
            def project = extractionResult.hubProjectName
            def version = extractionResult.hubProjectVersionName
            def components = extractionResult.bdioComponentDetailsList

            def outputFile = new File(workingDirectory, "${project}_${version}_bdio.jsonld")
            logger.info("Starting bdio creation using file: ${outputFile.canonicalPath} containing ${components.size()} components")
            new FileOutputStream(outputFile).withStream { outputStream ->
                def bdioWriter = bdioFileWriter.createBdioWriter(outputStream, project, version)
                try {
                    components.each { component ->
                        bdioFileWriter.writeComponent(bdioWriter, component)
                    }
                } finally {
                    bdioWriter.close()
                }
            }

            if (hubClient.isValid()) {
                hubClient.uploadBdioToHub(outputFile)
                logger.info("Uploaded bdio to ${hubClient.hubUrl}")
            }
        }
    }
}