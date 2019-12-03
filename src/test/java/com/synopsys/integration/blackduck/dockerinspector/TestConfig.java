package com.synopsys.integration.blackduck.dockerinspector;

import java.io.File;
import java.util.List;
import java.util.Map;

public class TestConfig {
    private String inspectTargetImageRepoTag;
    private String inspectTargetImageId;
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
    private boolean testSquashedImageGeneration;
    private Mode mode;
    private Map<String, String> env;
    private File outputContainerFileSystemFile;
    private File outputSquashedImageFile;
    private File targetTarInSharedDir;
    private long minContainerFileSystemFileSize;
    private long maxContainerFileSystemFileSize;
    private boolean appOnlyMode;


    public enum Mode {
        NO_SERVICE_START,
        DEFAULT,
        SPECIFY_II_DETAILS,
        DETECT
    }

    public TestConfig(final Mode mode, final String inspectTargetImageRepoTag, final String inspectTargetImageId, final String tarFilePath, final String targetRepo, final String targetTag, final int portOnHost, final boolean requireBdioMatch, final int minNumberOfComponentsExpected,
        final String outputBomMustContainComponentPrefix,
        final String outputBomMustNotContainComponentPrefix,
        final String outputBomMustContainExternalSystemTypeId, final String codelocationName, final List<String> additionalArgs, final Map<String, String> env, final boolean testSquashedImageGeneration,
        final File outputContainerFileSystemFile, final File outputSquashedImageFile, final File targetTarInSharedDir,
        final long minContainerFileSystemFileSize, final long maxContainerFileSystemFileSize, final boolean appOnlyMode) {
        this.mode = mode;
        this.inspectTargetImageRepoTag = inspectTargetImageRepoTag;
        this.inspectTargetImageId = inspectTargetImageId;
        this.tarFilePath = tarFilePath;
        this.targetRepo = targetRepo;
        this.targetTag = targetTag;
        this.portOnHost = portOnHost;
        this.requireBdioMatch = requireBdioMatch;
        this.minNumberOfComponentsExpected = minNumberOfComponentsExpected;
        this.outputBomMustContainComponentPrefix = outputBomMustContainComponentPrefix;
        this.outputBomMustNotContainComponentPrefix = outputBomMustNotContainComponentPrefix;
        this.outputBomMustContainExternalSystemTypeId = outputBomMustContainExternalSystemTypeId;
        this.codelocationName = codelocationName;
        this.additionalArgs = additionalArgs;
        this.env = env;
        this.testSquashedImageGeneration = testSquashedImageGeneration;
        this.outputContainerFileSystemFile = outputContainerFileSystemFile;
        this.outputSquashedImageFile = outputSquashedImageFile;
        this.targetTarInSharedDir = targetTarInSharedDir;
        this.minContainerFileSystemFileSize = minContainerFileSystemFileSize;
        this.maxContainerFileSystemFileSize = maxContainerFileSystemFileSize;
        this.appOnlyMode = appOnlyMode;
    }

    public Mode getMode() {
        return mode;
    }

    public String getInspectTargetImageRepoTag() {
        return inspectTargetImageRepoTag;
    }

    public String getInspectTargetImageId() {
        return inspectTargetImageId;
    }

    public String getTarFilePath() {
        return tarFilePath;
    }

    public String getTargetRepo() {
        return targetRepo;
    }

    public String getTargetTag() {
        return targetTag;
    }

    public int getPortOnHost() {
        return portOnHost;
    }

    public boolean isRequireBdioMatch() {
        return requireBdioMatch;
    }

    public int getMinNumberOfComponentsExpected() {
        return minNumberOfComponentsExpected;
    }

    public String getOutputBomMustContainComponentPrefix() {
        return outputBomMustContainComponentPrefix;
    }

    public String getOutputBomMustNotContainComponentPrefix() {
        return outputBomMustNotContainComponentPrefix;
    }

    public String getOutputBomMustContainExternalSystemTypeId() {
        return outputBomMustContainExternalSystemTypeId;
    }

    public String getCodelocationName() {
        return codelocationName;
    }

    public List<String> getAdditionalArgs() {
        return additionalArgs;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public boolean isTestSquashedImageGeneration() {
        return testSquashedImageGeneration;
    }

    public File getOutputContainerFileSystemFile() {
        return outputContainerFileSystemFile;
    }

    public File getOutputSquashedImageFile() {
        return outputSquashedImageFile;
    }

    public File getTargetTarInSharedDir() {
        return targetTarInSharedDir;
    }

    public long getMinContainerFileSystemFileSize() {
        return minContainerFileSystemFileSize;
    }

    public long getMaxContainerFileSystemFileSize() {
        return maxContainerFileSystemFileSize;
    }

    public boolean isAppOnlyMode() {
        return appOnlyMode;
    }

    public void setInspectTargetImageRepoTag(final String inspectTargetImageRepoTag) {
        this.inspectTargetImageRepoTag = inspectTargetImageRepoTag;
    }

    public void setTarFilePath(final String tarFilePath) {
        this.tarFilePath = tarFilePath;
    }

    public void setTargetRepo(final String targetRepo) {
        this.targetRepo = targetRepo;
    }

    public void setTargetTag(final String targetTag) {
        this.targetTag = targetTag;
    }

    public void setPortOnHost(final int portOnHost) {
        this.portOnHost = portOnHost;
    }

    public void setRequireBdioMatch(final boolean requireBdioMatch) {
        this.requireBdioMatch = requireBdioMatch;
    }

    public void setMinNumberOfComponentsExpected(final int minNumberOfComponentsExpected) {
        this.minNumberOfComponentsExpected = minNumberOfComponentsExpected;
    }

    public void setOutputBomMustContainComponentPrefix(final String outputBomMustContainComponentPrefix) {
        this.outputBomMustContainComponentPrefix = outputBomMustContainComponentPrefix;
    }

    public void setOutputBomMustNotContainComponentPrefix(final String outputBomMustNotContainComponentPrefix) {
        this.outputBomMustNotContainComponentPrefix = outputBomMustNotContainComponentPrefix;
    }

    public void setOutputBomMustContainExternalSystemTypeId(final String outputBomMustContainExternalSystemTypeId) {
        this.outputBomMustContainExternalSystemTypeId = outputBomMustContainExternalSystemTypeId;
    }

    public void setCodelocationName(final String codelocationName) {
        this.codelocationName = codelocationName;
    }

    public void setAdditionalArgs(final List<String> additionalArgs) {
        this.additionalArgs = additionalArgs;
    }

    public void setTestSquashedImageGeneration(final boolean testSquashedImageGeneration) {
        this.testSquashedImageGeneration = testSquashedImageGeneration;
    }

    public void setMode(final Mode mode) {
        this.mode = mode;
    }

    public void setEnv(final Map<String, String> env) {
        this.env = env;
    }

    public void setOutputContainerFileSystemFile(final File outputContainerFileSystemFile) {
        this.outputContainerFileSystemFile = outputContainerFileSystemFile;
    }

    public void setOutputSquashedImageFile(final File outputSquashedImageFile) {
        this.outputSquashedImageFile = outputSquashedImageFile;
    }

    public void setTargetTarInSharedDir(final File targetTarInSharedDir) {
        this.targetTarInSharedDir = targetTarInSharedDir;
    }

    public void setMinContainerFileSystemFileSize(final long minContainerFileSystemFileSize) {
        this.minContainerFileSystemFileSize = minContainerFileSystemFileSize;
    }

    public void setMaxContainerFileSystemFileSize(final long maxContainerFileSystemFileSize) {
        this.maxContainerFileSystemFileSize = maxContainerFileSystemFileSize;
    }

    public void setAppOnlyMode(final boolean appOnlyMode) {
        this.appOnlyMode = appOnlyMode;
    }
}
