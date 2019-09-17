package com.synopsys.integration.blackduck.dockerinspector;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.synopsys.integration.exception.IntegrationException;

public class TestConfigBuilder {
    private TestConfig.Mode mode = TestConfig.Mode.DEFAULT;
    private String inspectTargetImageRepoTag;
    private String tarFilePath;
    private String targetRepo; // tarfile image selector
    private String targetTag;  // tarfile image selector
    private int portOnHost;
    private boolean requireBdioMatch;
    private int minNumberOfComponentsExpected;
    private String outputBomMustContainComponentPrefix;
    private String outputBomMustNotContainComponentPrefix;
    private String outputBomMustContainExternalSystemTypeId;
    private String codelocationName;
    private List<String> additionalArgs;
    private Map<String, String> env;
    private boolean testSquashedImageGeneration;
    private File outputContainerFileSystemFile;
    private File outputSquashedImageFile;
    private File targetTarInSharedDir;
    private long minContainerFileSystemFileSize;
    private long maxContainerFileSystemFileSize;
    private boolean appOnlyMode;

    public TestConfigBuilder setAppOnlyMode(final boolean appOnlyMode) {
        this.appOnlyMode = appOnlyMode;
        return this;
    }

    public TestConfigBuilder setMode(final TestConfig.Mode mode) {
        this.mode = mode;
        return this;
    }

    public TestConfigBuilder setInspectTargetImageRepoTag(final String inspectTargetImageRepoTag) {
        this.inspectTargetImageRepoTag = inspectTargetImageRepoTag;
        return this;
    }

    public TestConfigBuilder setTarFilePath(final String tarFilePath) {
        this.tarFilePath = tarFilePath;
        return this;
    }

    public TestConfigBuilder setTargetRepo(final String targetRepo) {
        this.targetRepo = targetRepo;
        return this;
    }

    public TestConfigBuilder setTargetTag(final String targetTag) {
        this.targetTag = targetTag;
        return this;
    }

    public TestConfigBuilder setPortOnHost(final int portOnHost) {
        this.portOnHost = portOnHost;
        return this;
    }

    public TestConfigBuilder setRequireBdioMatch(final boolean requireBdioMatch) {
        this.requireBdioMatch = requireBdioMatch;
        return this;
    }

    public TestConfigBuilder setMinNumberOfComponentsExpected(final int minNumberOfComponentsExpected) {
        this.minNumberOfComponentsExpected = minNumberOfComponentsExpected;
        return this;
    }

    public TestConfigBuilder setOutputBomMustContainComponentPrefix(final String outputBomMustContainComponentPrefix) {
        this.outputBomMustContainComponentPrefix = outputBomMustContainComponentPrefix;
        return this;
    }

    public TestConfigBuilder setOutputBomMustNotContainComponentPrefix(final String outputBomMustNotContainComponentPrefix) {
        this.outputBomMustNotContainComponentPrefix = outputBomMustNotContainComponentPrefix;
        return this;
    }

    public TestConfigBuilder setOutputBomMustContainExternalSystemTypeId(final String outputBomMustContainExternalSystemTypeId) {
        this.outputBomMustContainExternalSystemTypeId = outputBomMustContainExternalSystemTypeId;
        return this;
    }

    public TestConfigBuilder setCodelocationName(final String codelocationName) {
        this.codelocationName = codelocationName;
        return this;
    }

    public TestConfigBuilder setAdditionalArgs(final List<String> additionalArgs) {
        this.additionalArgs = additionalArgs;
        return this;
    }

    public TestConfigBuilder setEnv(final Map<String, String> env) {
        this.env = env;
        return this;
    }

    public TestConfigBuilder setTestSquashedImageGeneration(final boolean testSquashedImageGeneration) {
        this.testSquashedImageGeneration = testSquashedImageGeneration;
        return this;
    }

    public TestConfigBuilder setOutputContainerFileSystemFile(final File outputContainerFileSystemFile) {
        this.outputContainerFileSystemFile = outputContainerFileSystemFile;
        return this;
    }

    public TestConfigBuilder setOutputSquashedImageFile(final File outputSquashedImageFile) {
        this.outputSquashedImageFile = outputSquashedImageFile;
        return this;
    }

    public TestConfigBuilder setTargetTarInSharedDir(final File targetTarInSharedDir) {
        this.targetTarInSharedDir = targetTarInSharedDir;
        return this;
    }

    public TestConfigBuilder setMinContainerFileSystemFileSize(final long minContainerFileSystemFileSize) {
        this.minContainerFileSystemFileSize = minContainerFileSystemFileSize;
        return this;
    }

    public TestConfigBuilder setMaxContainerFileSystemFileSize(final long maxContainerFileSystemFileSize) {
        this.maxContainerFileSystemFileSize = maxContainerFileSystemFileSize;
        return this;
    }

    public TestConfig build() throws IntegrationException {
        if ((inspectTargetImageRepoTag == null) && (tarFilePath == null)) {
            throw new IntegrationException("Invalid TestConfig");
        }
        return new TestConfig(mode, inspectTargetImageRepoTag, tarFilePath, targetRepo, targetTag, portOnHost, requireBdioMatch, minNumberOfComponentsExpected,
            outputBomMustContainComponentPrefix, outputBomMustNotContainComponentPrefix,
            outputBomMustContainExternalSystemTypeId, codelocationName, additionalArgs, env, testSquashedImageGeneration,
            outputContainerFileSystemFile, outputSquashedImageFile, targetTarInSharedDir, minContainerFileSystemFileSize, maxContainerFileSystemFileSize,
            appOnlyMode);
    }
}

