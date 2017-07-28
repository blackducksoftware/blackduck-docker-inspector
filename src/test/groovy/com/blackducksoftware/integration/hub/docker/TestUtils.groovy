package com.blackducksoftware.integration.hub.docker


class TestUtils {
    public static File createTempDirectory() throws IOException {
        final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if(!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if(!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }

    public static boolean contentEquals(File file1, File file2, List<String> exceptLinesContainingThese) {
        println "Comparing ${file1.getAbsolutePath()} and ${file2.getAbsolutePath()}"
        int ignoredLineCount = 0
        int matchedLineCount = 0
        List<String> lines1 = file1.readLines()
        List<String> lines2 = file2.readLines()

        if (lines1.size() != lines2.size()) {
            println ("Files' line counts are different")
            return false
        }
        for (int i=0; i < lines1.size(); i++) {
            String line1 = lines1.get(i)
            String line2 = lines2.get(i)
            boolean skip = false
            if (exceptLinesContainingThese != null) {
                for (String ignoreMe : exceptLinesContainingThese) {
                    if (line1.contains(ignoreMe) || line2.contains(ignoreMe)) {
                        skip = true
                        ignoredLineCount++
                    }
                }
            }
            if (skip) {
                continue
            }
            if (!line2.equals(line1)) {
                println "File comparison: These lines do not match:\n${lines1.get(i)}\n${lines2.get(i)}"
                return false
            } else {
                matchedLineCount++
            }
        }
        println "These files match (${matchedLineCount} lines matched; ${ignoredLineCount} lines ignored)"
        true
    }
}