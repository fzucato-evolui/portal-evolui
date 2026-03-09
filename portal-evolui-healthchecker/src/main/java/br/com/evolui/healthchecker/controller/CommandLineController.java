package br.com.evolui.healthchecker.controller;

import br.com.evolui.healthchecker.interfaces.IConsoleHandler;
import br.com.evolui.portalevolui.shared.dto.ConsoleResponseMessageDTO;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class CommandLineController {
    private ProcessBuilder builder = new ProcessBuilder();
    private Process process;
    private IConsoleHandler listener;
    private PrintWriter writer;
    private boolean isWindows = System.getProperty("os.name")
            .toLowerCase().startsWith("windows");
    public CommandLineController() {

    }
    public void executeCommand(String command) throws Exception {
        if (command.startsWith("cd ")) {
            String path = command.replace("cd ", "");
            Path p = Paths.get(path);
            if (path.equals("..")) {
                p = builder.directory().toPath();
                p = p.getParent();
            }
            else if (p.isAbsolute()) {
                builder.directory(p.toFile());
            }
            else {
                p = Paths.get(builder.directory().getAbsolutePath(), path);
                builder.directory(p.toFile());
            }
            if (p.toFile().exists()) {
                builder.directory(p.toFile());
            }
            if (this.listener != null) {
                ConsoleResponseMessageDTO dto = new ConsoleResponseMessageDTO();
                if (p.toFile().exists()) {
                    dto.setOutput(builder.directory().getAbsolutePath());
                } else {
                    dto.setOutputError(String.format("Diretório %s não existe", p.toString()));
                }
                if (builder.directory() != null) {
                    dto.setCurrentDirectory(builder.directory().getAbsolutePath());
                }
                dto.setSequence(0);
                dto.setFinished(true);
                listener.onInput(dto);
            }

        }
        else {
            if (command.equals("quit")) {
                if (this.listener != null) {
                    ConsoleResponseMessageDTO dto = new ConsoleResponseMessageDTO();
                    dto.setOutput("");
                    if (builder.directory() != null) {
                        dto.setCurrentDirectory(builder.directory().getAbsolutePath());
                    }
                    dto.setSequence(0);
                    dto.setFinished(true);
                    listener.onInput(dto);
                }
                this.dispose();
                return;
            }
            if (this.process == null || !this.process.isAlive()) {
                if (isWindows) {
                    builder.command("cmd.exe", "/c", command);
                } else {
                    builder.command("sh", "-c", command);
                }
                //builder.redirectErrorStream(true);
                //builder.inheritIO();
                this.process = builder.start();
                this.writer = new PrintWriter(this.process.getOutputStream());
                AtomicReference<SyncConsoleResponse> response = new AtomicReference(1);
                response.set(new SyncConsoleResponse(this.process));
                Future respOutput = newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {

                        BufferedReader reader = null;
                        if (response.get().isRunning()) {
                            reader = new BufferedReader(
                                    new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")));
                            while (response.get().isRunning()) {

                                try {
                                    if (response.get().getOutputLength() > 0 && listener != null) {
                                        ConsoleResponseMessageDTO dto = new ConsoleResponseMessageDTO();
                                        dto.setOutput(response.get().getOutput());
                                        if (builder.directory() != null) {
                                            dto.setCurrentDirectory(builder.directory().getAbsolutePath());
                                        }
                                        dto.setSequence(response.get().getSequence());
                                        dto.setFinished(false);
                                        listener.onInput(dto);
                                        response.get().deleteOutput();
                                    }
                                    if (reader.ready()) {
                                        response.get().deleteOutput();
                                        String line;
                                        Integer count = 0;
                                        while (reader.ready() && (line = reader.readLine()) != null) {
                                            response.get().appendOutput(line + System.lineSeparator());
                                            count++;
                                            if (count > 20) {
                                                break;
                                            }
                                        }
                                    }

                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        response.get().setFinishedOutput();
                        if (listener != null) {
                            if (response.get().isFinished()) {
                                ConsoleResponseMessageDTO dto = new ConsoleResponseMessageDTO();

                                dto.setOutput(response.get().getOutput());
                                dto.setOutputError(response.get().getOutputError());
                                if (builder.directory() != null) {
                                    dto.setCurrentDirectory(builder.directory().getAbsolutePath());
                                }
                                dto.setSequence(response.get().getSequence());
                                dto.setFinished(true);

                                listener.onInput(dto);
                            }

                        }
                        try {
                            reader.close();;
                        } catch (Exception ex) {

                        }
                        try {
                            process.destroy();;
                        } catch (Exception ex) {

                        } finally {
                            process = null;
                        }
                        try {
                            writer.close();;
                        } catch (Exception ex) {

                        }
                        process = null;
                        writer = null;
                    }
                });
                Future respError = newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {

                        BufferedReader reader = null;
                        if (response.get().isRunning()) {
                            reader = new BufferedReader(
                                    new InputStreamReader(process.getErrorStream(), Charset.forName("UTF-8")));
                            while (response.get().isRunning()) {

                                try {
                                    if (response.get().getOutputErrorLength() > 0 && listener != null) {
                                        ConsoleResponseMessageDTO dto = new ConsoleResponseMessageDTO();
                                        dto.setOutputError(response.get().getOutputError());
                                        if (builder.directory() != null) {
                                            dto.setCurrentDirectory(builder.directory().getAbsolutePath());
                                        }
                                        dto.setSequence(response.get().getSequence());
                                        dto.setFinished(false);
                                        listener.onError(dto);
                                        response.get().deleteOutputError();
                                    }
                                    if (reader.ready()) {
                                        response.get().deleteOutputError();
                                        String line;
                                        Integer count = 0;
                                        while (reader.ready() && (line = reader.readLine()) != null) {
                                            response.get().appendOutputError(line + System.lineSeparator());
                                            count++;
                                            if (count > 20) {
                                                break;
                                            }
                                        }
                                    }

                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        response.get().setFinishedError();
                        if (listener != null) {
                            if (response.get().isFinished()) {
                                ConsoleResponseMessageDTO dto = new ConsoleResponseMessageDTO();

                                dto.setOutput(response.get().getOutput());
                                dto.setOutputError(response.get().getOutputError());
                                if (builder.directory() != null) {
                                    dto.setCurrentDirectory(builder.directory().getAbsolutePath());
                                }
                                dto.setSequence(response.get().getSequence());
                                dto.setFinished(true);

                                listener.onInput(dto);
                            }

                        }
                        try {
                            reader.close();
                        } catch (Exception ex) {

                        }
                        try {
                            process.destroy();;
                        } catch (Exception ex) {

                        } finally {
                            process = null;
                        }
                        try {
                            writer.close();;
                        } catch (Exception ex) {

                        }
                        process = null;
                        writer = null;
                    }
                });


            }
            else {

                this.writer.write(command + System.lineSeparator());
                this.writer.flush();
            }
        }
    }

    public String executeSingleCommand(String command, Long waitTimeSeconds) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        final Process process = builder.start();

        Future<String> success = newSingleThreadExecutor().submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")));
                final StringBuilder output = new StringBuilder();
                try {
                    while (process != null && process.isAlive()) {

                        if (reader.ready()) {
                            String line;
                            while (reader.ready() && (line = reader.readLine()) != null) {
                                output.append(line + System.lineSeparator());
                            }
                        }
                    }
                    return output.toString();
                } catch (Throwable ex) {
                    throw ex;
                } finally {
                    try {
                        reader.close();
                        ;
                    } catch (Exception ex) {

                    }
                    try {
                        process.destroy();
                        ;
                    } catch (Exception ex) {
                    }

                }
            }

        });
        Future<String> error = newSingleThreadExecutor().submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), Charset.forName("UTF-8")));
                final StringBuilder output = new StringBuilder();
                try {
                    while (process != null && process.isAlive()) {

                        if (reader.ready()) {
                            String line;
                            while (reader.ready() && (line = reader.readLine()) != null) {
                                output.append(line + System.lineSeparator());
                            }
                        }
                    }
                    return output.toString();
                } catch (Throwable ex) {
                    throw ex;
                } finally {
                    try {
                        reader.close();
                        ;
                    } catch (Exception ex) {

                    }
                    try {
                        process.destroy();
                        ;
                    } catch (Exception ex) {
                    }

                }
            }

        });


        if (waitTimeSeconds != null) {
            String successResp = success.get(waitTimeSeconds, TimeUnit.SECONDS);
            String errorResp = error.get(waitTimeSeconds, TimeUnit.SECONDS);
            if (StringUtils.hasText(errorResp)) {
                throw new Exception(errorResp);
            }
            return successResp;
        } else {
            String successResp = success.get(waitTimeSeconds, TimeUnit.SECONDS);
            String errorResp = error.get(waitTimeSeconds, TimeUnit.SECONDS);
            if (StringUtils.hasText(errorResp)) {
                throw new Exception(errorResp);
            }
            return successResp;
        }


    }

    public void dispose() {
        try {
            process.destroy();;
        } catch (Exception ex) {

        } finally {
            process = null;
        }
        try {
            writer.close();;
        } catch (Exception ex) {

        } finally {
            writer = null;
        }
        process = null;
        writer = null;
    }

    public void restart() {
        this.dispose();
        this.builder.directory(new File(System.getProperty("user.home")));
    }
    public void setListener(IConsoleHandler listener) {
        this.listener = listener;
    }

    private class SyncConsoleResponse {
        Process process;
        private boolean finishedOutput = false;
        private boolean finishedError = false;
        private StringBuilder output = new StringBuilder();
        private StringBuilder outputError = new StringBuilder();
        private boolean hadOutput = false;
        private boolean hadError = false;
        private int sequence = -1;
        public SyncConsoleResponse(Process p) {
            this.process = p;
        }

        public boolean isRunning() {
            try {
                return this.process != null && (this.process.isAlive() || this.process.getInputStream().available() > 0 || this.process.getErrorStream().available() > 0);
            } catch (IOException e) {
                return false;
            }
        }

        public boolean isFinished() {
            return this.finishedError && this.finishedOutput;
        }


        public String getOutput() {
            return output.toString();
        }

        public int getOutputLength() {
            return output.length();
        }

        public void appendOutput(String output) {
            this.output.append(output);
            if (StringUtils.hasText(output)) {
                this.hadOutput = true;
            }
        }

        public void deleteOutput() {
            this.output.delete(0, output.length());
        }

        public String getOutputError() {
            return outputError.toString();
        }

        public void appendOutputError(String output) {
            this.outputError.append(output);
            if (StringUtils.hasText(output)) {
                this.hadError = true;
            }
        }

        public void deleteOutputError() {
            this.outputError.delete(0, outputError.length());
        }

        public int getOutputErrorLength() {
            return outputError.length();
        }

        public boolean isHadOutput() {
            return hadOutput;
        }

        public boolean isHadError() {
            return hadError;
        }

        public void setFinishedOutput() {
            this.finishedOutput = true;
        }

        public void setFinishedError() {
            this.finishedError = true;
        }

        public int getSequence() {
            this.sequence++;
            return this.sequence;
        }

    }

}
