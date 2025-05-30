/*
 * Copyright 2024 Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */
package tck.arquillian.protocol.appclient;

import org.jboss.arquillian.config.impl.extension.StringPropertyReplacer;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A class that uses the {@link Runtime#exec(String[], String[], File)} method to launch a Jakarta EE appclient.
 * Vendors configure the appclient via the {@link AppClientProtocolConfiguration} class using the
 * protocol element of the arquillian.xml file.
 */
public class AppClientCmd {
    /**
     *
     * @param deploymentName - the name of the deployment (top archive name or @Deployment name)
     * @param vehicleArchiveName - the name of the vehicle archive to pass to the app client
     * @param clientAppArchive - the appclient archive
     * @param clientStubJar - optional client stub jar, may be null
     */
    record AppClientInfo(String deploymentName, String vehicleArchiveName, String clientAppArchive, String clientStubJar) {}

    private static final Logger LOGGER = Logger.getLogger(AppClientCmd.class.getName());

    private static final String outThreadHame = "APPCLIENT-out";
    private static final String errThreadHame = "APPCLIENT-err";

    private Process appClientProcess;
    private BufferedReader outputReader;
    private BufferedReader errorReader;
    private BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();
    private String[] clientCmdLine = {};
    private String[] clientEnvp = null;
    private File clientDir = null;
    private String clientEarDir;
    private String clientEarLibClasspath;
    private CompletableFuture<Process> onExit;
    @Inject
    @DeploymentScoped
    private Instance<AppClientProtocolConfiguration> packagerConfigInstance;

    /**
     * Obtain the AppClientProtocolConfiguration from the injected producer to determine the clientCmdLine,
     * optional clientEnvp and optional clientDir, clientEarDir, and client classpath.
     */
    public void init() {
        AppClientProtocolConfiguration config = packagerConfigInstance.get();
        clientCmdLine = config.clientCmdLineAsArray();
        clientEnvp = config.clientEnvAsArray();
        clientDir = config.clientDirAsFile();
        clientEarDir = config.getClientEarDir();
        clientEarLibClasspath = config.clientEarLibClasspath();
    }

    public AppClientProtocolConfiguration getConfig() {
        return packagerConfigInstance.get();
    }

    public boolean waitForExit(long timeout, TimeUnit units) throws InterruptedException {
        return appClientProcess.waitFor(timeout, units);
    }

    /**
     * Consumes all available output from App Client using the output queue filled by the process
     * stanard out reader thread.
     *
     * @param timeout number of milliseconds to wait for each subsequent line
     * @return array of App Client output lines
     */
    public String[] readAll(final long timeout) {
        ArrayList<String> lines = new ArrayList<String>();
        String line = null;
        do {
            try {
                line = outputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (line != null)
                    lines.add(line);
            } catch (InterruptedException ioe) {
            }

        } while (onExit.isDone() == false);
        return lines.toArray(new String[] {});
    }

    /**
     * Kills the app client
     *
     * @throws Exception
     */
    public synchronized void quit() throws Exception {
        appClientProcess.destroy();
        try {
            appClientProcess.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts the app client in a new process and creates two threads to read the process output
     * and error streams.
     * @param appClientInfo - Info about the client app to run.
     * @param additionalArgs - additional arguments passed to the app client process. The CTS appclient will
     *                       pass in the name of the test to run using this.
     * @throws Exception - on failure
     */
    public void run(AppClientInfo appClientInfo, String... additionalArgs) throws Exception {
        
        ArrayList<String> cmdList = new ArrayList<String>();
        String clientAppArchiveName = appClientInfo.clientAppArchive;
        if(clientAppArchiveName.endsWith(".jar")) {
            clientAppArchiveName = clientAppArchiveName.substring(0, clientAppArchiveName.length() - 4);
        }
        // Need to replace any property refs on command line
        File earDir = new File(clientEarDir);
        if(earDir.isAbsolute()) {
            earDir = new File(clientDir, clientEarDir);
        }
        String[] cmdLine = Arrays.copyOf(clientCmdLine, clientCmdLine.length);
        for (int n = 0; n < cmdLine.length; n ++) {
            String arg = cmdLine[n];
            if(arg.contains("${deploymentName}")) {
                arg = arg.replaceAll("\\$\\{deploymentName}", appClientInfo.deploymentName);
                cmdLine[n] = arg;
            }
            if(arg.contains("${clientEarDir}")) {
                arg = arg.replaceAll("\\$\\{clientEarDir}", earDir.getAbsolutePath());
                cmdLine[n] = arg;
            }
            if(arg.contains("${vehicleArchiveName}")) {
                arg = arg.replaceAll("\\$\\{vehicleArchiveName}", appClientInfo.vehicleArchiveName);
                cmdLine[n] = arg;
            }
            if(arg.contains("${clientAppArchive}")) {
                arg = arg.replaceAll("\\$\\{clientAppArchive}", appClientInfo.clientAppArchive);
                cmdLine[n] = arg;
            }
            if(arg.contains("${clientAppArchiveName}")) {
                arg = arg.replaceAll("\\$\\{clientAppArchiveName}", clientAppArchiveName);
                cmdLine[n] = arg;
            }
            if(arg.contains("${clientEarLibClasspath}")) {
                arg = arg.replaceAll("\\$\\{clientEarLibClasspath}", clientEarLibClasspath);
                cmdLine[n] = arg;
            }
            if(arg.contains("${clientStubJar}")) {
                arg = arg.replaceAll("\\$\\{clientStubJar}", appClientInfo.clientStubJar);
                cmdLine[n] = arg;
            }
        }
        // Replace any ${clientEarLibClasspath} in the client ENV
        for(int n = 0; n < clientEnvp.length; n++) {
            String env = clientEnvp[n];
            if(env.contains("${clientEarLibClasspath}")) {
                String env2 = env.replaceAll("\\$\\{clientEarLibClasspath}", clientEarLibClasspath);
                LOGGER.info("Replaced clientEarLibClasspath in "+env2);
                clientEnvp[n] = env2;
            }
        }

        // Split the command line into individual arguments based on spaces
        for (int n = 0; n < cmdLine.length; n ++) {
            cmdList.addAll(Arrays.asList(cmdLine[n].split(" ")));
        }

        // Add any additional args
        if (additionalArgs != null) {
            String[] newCmdLine = new String[cmdLine.length + additionalArgs.length];
            System.arraycopy(cmdLine, 0, newCmdLine, 0, cmdLine.length);
            System.arraycopy(additionalArgs, 0, newCmdLine, cmdLine.length, additionalArgs.length);
            cmdList.addAll(Arrays.asList(additionalArgs));
        }

        appClientProcess = Runtime.getRuntime().exec(cmdList.toArray(new String[0]), clientEnvp, clientDir);
        onExit = appClientProcess.onExit();
        LOGGER.info("Created process" + appClientProcess.info());
        LOGGER.info("process(%d).envp: %s".formatted(appClientProcess.pid(), Arrays.toString(clientEnvp)));
        outputReader = new BufferedReader(new InputStreamReader(appClientProcess.getInputStream(), StandardCharsets.UTF_8));
        errorReader = new BufferedReader(new InputStreamReader(appClientProcess.getErrorStream(), StandardCharsets.UTF_8));

        final Thread readOutputThread = new Thread(this::readClientOut, outThreadHame);
        readOutputThread.start();
        final Thread readErrorThread = new Thread(this::readClientErr, errThreadHame);
        readErrorThread.start();
        LOGGER.info("Started process reader threads");
    }

    private void readClientOut() {
        if (outputReader == null)
            return;

        readClientProcess(outputReader, false);
        synchronized (this) {
            outputReader = null;
        }
    }

    private void readClientErr() {
        if (errorReader == null)
            return;
        readClientProcess(errorReader, true);
        synchronized (this) {
            errorReader = null;
        }
    }

    /**
     * Loop
     */
    private void readClientProcess(BufferedReader reader, boolean errReader) {
        LOGGER.info("Begin readClientProcess");
        int count = 0;
        try {
            String line = reader.readLine();
            // System.out.println("RCP: " + line);
            while (line != null) {
                count++;
                if (errReader)
                    errorLineReceived(line);
                else
                    outputLineReceived(line);
                line = reader.readLine();
            }
        } catch (Throwable e) {
            LOGGER.warning(formatException("error during read, caused by:\n", e));
        }
        LOGGER.info(String.format("Exiting(%s), read %d lines", errReader, count));
    }

    private synchronized void outputLineReceived(String line) {
        LOGGER.info("[" + outThreadHame + "] " + line);
        outputQueue.add(line);
    }

    private synchronized void errorLineReceived(String line) {
        LOGGER.info("[" + errThreadHame + "] " + line);
        outputQueue.add(line);
    }

    private static String formatException(String msg, Throwable e) {
        StringWriter sw = new StringWriter();
        sw.append(msg);
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }
}
