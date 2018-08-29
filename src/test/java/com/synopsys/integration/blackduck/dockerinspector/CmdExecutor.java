package com.synopsys.integration.blackduck.dockerinspector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.synopsys.integration.exception.IntegrationException;

public class CmdExecutor {
    public static String execCmd(final String cmd, final long timeoutSeconds, final Map<String, String> env) throws IOException, InterruptedException, IntegrationException {
        System.out.println(String.format("Executing: %s", cmd));
        final ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
        pb.redirectOutput(Redirect.PIPE);
        pb.redirectError(Redirect.PIPE);
        pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
        if (env != null) {
            pb.environment().putAll(env);
        }
        final Process p = pb.start();
        final boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            throw new InterruptedException("Command timed out");
        }
        final int retCode = p.exitValue();
        if (retCode != 0) {
            final String stderr = toString(p.getErrorStream());
            System.out.println(String.format("%s: stderr: '%s'", cmd, stderr));
            throw new IntegrationException(String.format("Command failed: %s", stderr));
        }
        return toString(p.getInputStream());
    }

    private static String toString(final InputStream is) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
        }
        return builder.toString();
    }
}
