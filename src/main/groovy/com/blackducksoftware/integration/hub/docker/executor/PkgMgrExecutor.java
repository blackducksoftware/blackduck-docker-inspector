package com.blackducksoftware.integration.hub.docker.executor;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.docker.PackageManagerFiles;
import com.blackducksoftware.integration.hub.docker.tar.ImagePkgMgr;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

public abstract class PkgMgrExecutor extends Executor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private String upgradeCommand;
    private String listPackagesCommand;

    public abstract void init();

    void initValues(final String upgradeCommand, final String listPackagesCommand) {
        this.upgradeCommand = upgradeCommand;
        this.listPackagesCommand = listPackagesCommand;
    }

    public String[] runPackageManager(final ImagePkgMgr imagePkgMgr) throws HubIntegrationException, IOException, InterruptedException {
        final PackageManagerFiles pkgMgrFiles = new PackageManagerFiles();
        pkgMgrFiles.stubPackageManagerFiles(imagePkgMgr);
        final String[] packages = listPackages();
        logger.trace(String.format("Package count: %d", packages.length));
        return packages;
    }

    private String[] listPackages() throws HubIntegrationException, IOException, InterruptedException {
        String[] results;
        logger.debug("Executing package manager");
        try {
            results = executeCommand(listPackagesCommand);
            logger.info(String.format("Command %s executed successfully", listPackagesCommand));
        } catch (final Exception e) {
            if (!StringUtils.isBlank(upgradeCommand)) {
                logger.warn(String.format("Error executing \"%s\": %s; Trying to upgrade package database by executing: %s", listPackagesCommand, e.getMessage(), upgradeCommand));
                executeCommand(upgradeCommand);
                results = executeCommand(listPackagesCommand);
                logger.info(String.format("Command %s executed successfully on 2nd attempt (after db upgrade)", listPackagesCommand));
            } else {
                logger.error(String.format("Error executing \"%s\": %s; No upgrade command has been provided for this package manager", listPackagesCommand, e.getMessage()));
                throw e;
            }
        }
        logger.debug(String.format("Package manager reported %s package lines", results.length));
        return results;
    }

}
