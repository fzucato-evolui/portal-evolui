package br.com.evolui.healthchecker.controller;

import br.com.evolui.healthchecker.converter.WebsocketMessageConverter;
import br.com.evolui.healthchecker.dto.ParameterDTO;
import br.com.evolui.healthchecker.enums.ActionType;
import br.com.evolui.healthchecker.exceptions.WebClientStatusException;
import br.com.evolui.healthchecker.interfaces.IConsoleHandler;
import br.com.evolui.healthchecker.util.UtilFunctions;
import br.com.evolui.portalevolui.shared.dto.*;
import br.com.evolui.portalevolui.shared.enums.HealthCheckerMessageTopicConstants;
import br.com.evolui.portalevolui.shared.util.GeradorTokenPortalEvolui;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class MainController extends StompSessionHandlerAdapter implements Runnable, IConsoleHandler {
    private static Logger logger = LoggerFactory.getLogger(MainController.class);
    //public static String ANSI_RESET = "\u001B[0m";
    //public static String ANSI_YELLOW = "\u001B[33m";
    //public static String ANSI_CYAN = "\u001B[36m";
    //public static String ANSI_RED = "\u001B[31m";
    private ParameterDTO params;
    private Console console = System.console();
    private Scanner scanner = new Scanner(System.in);
    private LocalDateTime lastReceivedMessage = LocalDateTime.now();
    private String destination;
    private HealthCheckerConfigDTO config;
    private ObjectMapper mapper;
    private SystemInfoRuntime si;
    private ExecutorService executorService;
    private ListenableFuture<StompSession> websession;
    private CommandLineController commandLineController;
    private String token;
    private StompHeaders stompHeaders;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledResult;
    private AtomicBoolean exit = new AtomicBoolean(false);
    private String identifier;
    public static boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
    public MainController(String[] args) {
        try {
            this.params = ParameterDTO.build(args);
        } catch (Exception ex) {

        }
    }

    @Override
    public void run() {
        try {

        } catch (Exception ex) {
            printMenu();
            return;
        }
        try {
            this.mapper = new ObjectMapper();
            this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            this.commandLineController = new CommandLineController();
            this.commandLineController.setListener(this);
            this.executorService = newFixedThreadPool(2);
            this.si = new SystemInfoRuntime();
            Calendar now = Calendar.getInstance();
            logger.info("Lendo configurações de hardware e software..." + (isLinux ? " (Linux)" : ""));
            HealthCheckerSystemInfoDTO siDTO = this.readSystemInfo(false);
            if (siDTO.getOperatingSystem().isElevated() == false) {
                throw new Exception("Deve ser executado como administrador (todos os privilégios)");
            }
            Calendar after = Calendar.getInstance();
            Long milis = after.getTimeInMillis() - now.getTimeInMillis();
            logger.info(TimeUnit.MILLISECONDS.toSeconds(milis) + " segundos");
            Files.write(Paths.get("completeSI2.json"), this.mapper.writeValueAsBytes(siDTO));
            this.identifier = siDTO.getHardware().getComputerSystem().getHardwareUUID();
            start();
            executorService.submit(() -> {
                try {
                    while (!exit.get()) {
                        Thread.sleep(1000);
                        try {
                            if (checkNeedRunScheduler()) {
                                logger.info(String.format("Scheduler não foi executado por mais de %s minutos. Reiniciando...", this.config.getHealthCheckInterval() * 3));
                                runScheduler();
                            }
                        } catch (Throwable ex) {
                            logger.error(UtilFunctions.exceptionToString(ex));
                        }
                    }
                } catch (Throwable ex) {
                    logger.error(UtilFunctions.exceptionToString(ex));
                }
            }).get();


        } catch (Throwable ex) {
            //logger.severe(UtilFunctions.exceptionToString(ex));
            throw new RuntimeException(ex);
        }
    }

    private void start() throws Throwable {
        synchronized (this) {
            while (params == null || params.getActionType() == null) {
                printMenu();
                String line = scanner.nextLine();

                try {
                    int option = Integer.parseInt(line);
                    ActionType a = ActionType.values()[option - 1];
                    if (a == null) {
                        throw new Exception("Opção inválida");
                    }
                    params = new ParameterDTO();
                    params.setActionType(a);
                } catch (Exception ex) {
                    System.err.println("Opção inválida");
                }

            }
            if (params.getActionType() == ActionType.init) {
                init();
            } else if (params.getActionType() == ActionType.run) {
                try {
                    this.config = UtilController.readConfiguration();
                } catch (Exception ex) {
                    throw new RuntimeException("Erro ao ler configurações");
                }
                this.runScheduler();
            }
        }
    }

    private void printMenu(){
        System.out.println("*****************************************************************************");
        System.out.println("Selecione uma opção");
        System.out.println("1. init: Para configuração inicial do Health Checker");
        System.out.println("2. run: Para iniciar a verificação (Health Check) do sistema");
        System.out.println("*****************************************************************************");
    }

    private HealthCheckerSystemInfoDTO readSystemInfo(boolean update) throws Exception{
        this.si.setForceUpdate(update);
        HealthCheckerSystemInfoDTO siDTO = this.executorService.submit(this.si).get();
        return siDTO;
    }

    private void init() throws Throwable {
        System.out.println("*****************************************************************************");
        System.out.println("Cole o token exibido no portal");
        System.out.println("*****************************************************************************");
        String token = null;
        while (!(StringUtils.hasText(token))) {
            token = scanner.nextLine();
        }
        String json = GeradorTokenPortalEvolui.decrypt(token);
        HealthCheckerConnectionDTO connectionDTO = this.mapper.readValue(json, HealthCheckerConnectionDTO.class);
        destination = connectionDTO.getDestination();
        config = new HealthCheckerConfigDTO();
        config.setHost(connectionDTO.getHost());
        config.setLogin(connectionDTO.getLogin());
        config.setIdentifier(this.getIdentifier());
        this.startWebSocket();
        this.websession.get();
        if (this.isWebsocketConnected(20L)) {
            this.sendMessage(null, HealthCheckerMessageTopicConstants.HEY);
        }
    }

    private boolean checkNeedRunScheduler() {
        synchronized (this) {
            if (!exit.get() && scheduledResult != null && scheduledResult.isDone() && this.params.getActionType() == ActionType.run) {
                return Duration.between(this.lastReceivedMessage, LocalDateTime.now()).toMinutes() > this.config.getHealthCheckInterval() * 3;
            }
            return false;
        }
    }

    private void runScheduler() throws Throwable {
        synchronized (this) {
            logger.info("Start Scheduler");
            boolean isClosed = this.websession == null || !websession.isDone();
            if (!isClosed) {
                try {
                    StompSession session = websession.get();
                    isClosed = !session.isConnected();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    isClosed = true;
                }
            }
            if (isClosed) {
                try {
                    if (websession != null) {
                        websession.cancel(true);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    this.startWebSocket();
                    this.websession.get();
                } catch (Exception ex) {
                    logger.error(UtilFunctions.exceptionToString(ex));
                }
            }
            try {
                stopScheduler(false);
            } catch (Throwable ex) {

            }
            this.commandLineController.dispose();
            this.destination = null;
            scheduledResult = this.scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        logger.info("run Scheduler "  + Thread.currentThread().getId());
                        //logger.info("schedulerSize " + ((ScheduledThreadPoolExecutor) scheduler).getQueue().size());
                        HealthCheckController controller = new HealthCheckController(config);
                        HealthCheckerSystemInfoDTO siDTO = readSystemInfo(true);
                        Thread.sleep(1000); // Para ler o uso da CPU
                        siDTO = readSystemInfo(true);
                        HealthCheckerDTO dto = controller.check(siDTO);
                        try {
                            WebClientController client = new WebClientController(config);
                            client.saveChecker(null, dto);

                        } catch (WebClientStatusException exception) {
                            if (exception.getStatus().getStatusCode() == 403) {
                                logger.error(UtilFunctions.exceptionToString(exception));
                                UtilController.deleteConfigFile();
                                try {
                                    stopScheduler(true);
                                } catch (Throwable ex) {

                                }
                                return;
                            }
                        }

                    } catch (Throwable e) {
                        logger.error(UtilFunctions.exceptionToString(e));
                    }
                    finally {
                        System.gc();
                    }
                    // Mandou o staus do healthchecker, mas está desconectado do websocket. Tenta se reconectar
                    try {
                        if (!isWebsocketConnected(40L)) {
                            try {
                                websession.cancel(true);
                            } catch (Exception e) {

                            }
                            startWebSocket();
                        }

                    } catch (Throwable ex) {
                        logger.error(UtilFunctions.exceptionToString(ex));
                    }
                }
            }, this.config.getHealthCheckInterval(), this.config.getHealthCheckInterval(), TimeUnit.MINUTES);
        }
    }

    private void runSchedulerSI() throws Throwable {
        synchronized (this) {
            try {
                this.stopScheduler(false);
            } catch (Throwable ex) {

            }
            scheduledResult = this.scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        //logger.info("run runSchedulerSI " +  Thread.currentThread().getId());
                        //logger.info("schedulerSize " + ((ScheduledThreadPoolExecutor) scheduler).getQueue().size());
                        HealthCheckerSystemInfoDTO siDTO = readSystemInfo(true);
                        sendMessage(siDTO, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, null);
                    } catch (Throwable e) {
                        if (!isWebsocketConnected(20L)) {
                            try {
                                runScheduler();
                            } catch (Throwable ex) {
                                logger.error(UtilFunctions.exceptionToString(e));
                            }
                        } else if (!(e instanceof InterruptedException)) {
                            try {
                                sendMessage(null, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, e);
                            } catch (Throwable ex) {
                                logger.error(UtilFunctions.exceptionToString(ex));
                            }
                            logger.error(UtilFunctions.exceptionToString(e));
                        }
                    }
                    finally {
                        System.gc();
                    }
                }
            }, 0, 2, TimeUnit.SECONDS);
        }
    }
    private boolean isWebsocketConnected(Long seconds) {
        synchronized (this) {
            try {
                if (!websession.isDone()) {
                    return false;
                }
                StompSession session = websession.get(seconds, TimeUnit.SECONDS);
                if (session == null || !session.isConnected()) {
                    return false;
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

    }
    private void startWebSocket() throws Throwable {
        synchronized (this) {
            WebClientController c = new WebClientController(config);
            this.token = c.getToken();
            URI url = new URI(this.config.getHost());
            URI urlWS = new URI(url.getScheme().equals("https") ? "wss" : "ws", url.getUserInfo(), url.getHost(), url.getPort(), "/portalEvoluiWebSocket", null, null);
            ClientManager client = ClientManager.createClient();

            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(UtilController.getSSLContext());
            sslEngineConfigurator.setHostVerificationEnabled(false); //skip host verification

            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            client.setDefaultMaxTextMessageBufferSize(10000 * 1024);

            StandardWebSocketClient webSocketClient = new StandardWebSocketClient(client);
            WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
            stompClient.setInboundMessageSizeLimit(10000 * 1024);
            stompClient.setMessageConverter(new WebsocketMessageConverter());

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.setBearerAuth(this.token);

            this.stompHeaders = new StompHeaders();
            stompHeaders.add("Authorization", "Bearer " + token);

            headers.add("Identifier", this.getIdentifier());
            websession = stompClient.connect(urlWS.toString(), headers, stompHeaders, this);
        }
    }

    private void stopWebSocket() {
        synchronized (this) {
            try {
                stopScheduler(false);
            } catch (Throwable ex) {

            }
            try {
                this.websession.get().disconnect();
            } catch (Exception ex) {

            }
            this.commandLineController = null;
        }
    }

    private<T> void sendMessage(T message, String destination) throws Throwable {

        this.sendMessage(message, destination, null);
    }

    private<T> void sendMessage(T message, String destination, Throwable error) throws Throwable {
        this.sendMessage(message, destination, this.destination, error);
    }

    private<T> void sendMessage(T message, String destination, String to, Throwable error) throws Throwable {
        WebSocketMessageDTO<T> m = new WebSocketMessageDTO<T>();
        stompHeaders.setDestination(String.format("/app/%s", destination));
        m.setTo(to);
        m.setFrom(this.getIdentifier());
        m.setMessage(message);
        if (error != null) {
            m.setError(UtilFunctions.exceptionToString(error));
        }
        websession.get().send(stompHeaders, m);

    }

    private String getIdentifier() {
        return this.identifier;
    }
    private void stopScheduler(boolean exit) {
        synchronized (this) {
            logger.info("Stop Scheduler");
            //Thread.dumpStack();
            if (scheduledResult != null) {
                scheduledResult.cancel(true);
            }
            if (exit == true) {
                try {
                    this.scheduler.shutdown();
                }
                catch (Throwable e) {
                    logger.error(UtilFunctions.exceptionToString(e));
                }
            }
            this.exit.set(exit);
        }
    }


    @Override
    public Type getPayloadType(StompHeaders headers) {

        return String.class;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        session.subscribe(String.format("/topic/%s", HealthCheckerMessageTopicConstants.CLIENT_DISCONECTION), this);
        session.subscribe(String.format("/queue/%s/%s", HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST,this.getIdentifier()), this);
        session.subscribe(String.format("/queue/%s/%s", HealthCheckerMessageTopicConstants.START_REQUEST,this.getIdentifier()), this);
        session.subscribe(String.format("/queue/%s/%s", HealthCheckerMessageTopicConstants.TEST_CONFIG_REQUEST,this.getIdentifier()), this);
        session.subscribe(String.format("/queue/%s/%s", HealthCheckerMessageTopicConstants.SAVE_CONFIG_REQUEST,this.getIdentifier()), this);
        session.subscribe(String.format("/queue/%s/%s", HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START,this.getIdentifier()), this);
        session.subscribe(String.format("/queue/%s/%s", HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP,this.getIdentifier()), this);

    }
    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        this.lastReceivedMessage = LocalDateTime.now();
        if (payload == null) {
            return;
        }
        //logger.info(headers.getDestination());
        if (headers.getDestination().contains(HealthCheckerMessageTopicConstants.CLIENT_DISCONECTION)) {
            try {
                LinkedHashMap<String, String> msg = this.mapper.readValue(payload.toString(), new TypeReference<LinkedHashMap<String, String>>() {});
                if (StringUtils.hasText(this.destination) && this.destination.equals(msg.get("client"))) {
                    this.commandLineController.dispose();
                    if (this.params.getActionType() == ActionType.init) {
                        System.err.println("Destinatário encerrou a conexão");
                        this.stopWebSocket();
                        this.start();
                    } else if (this.params.getActionType() == ActionType.run) {
                        this.runScheduler();
                    }
                }
            } catch (Throwable e) {
                logger.error(UtilFunctions.exceptionToString(e));
            }
        }
        else if (headers.getDestination().contains(HealthCheckerMessageTopicConstants.START_REQUEST)) {
            try {

                try {
                    this.stopScheduler(false);
                } catch (Throwable ex) {

                }
                try {
                    this.commandLineController.restart();
                } catch (Throwable ex) {

                }
                WebSocketMessageDTO msg = this.mapper.readValue(payload.toString(), WebSocketMessageDTO.class);
                if (StringUtils.hasText(this.destination) && !this.destination.equals(msg.getFrom())) {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.START_RESPONSE, msg.getFrom(), new Throwable("HealthChecker ocupado"));
                } else {
                    this.destination = msg.getFrom();
                    HealthCheckerSystemInfoDTO dto = this.readSystemInfo(false);
                    this.sendMessage(dto, HealthCheckerMessageTopicConstants.START_RESPONSE);
                }
            } catch (Throwable e) {
                logger.error(UtilFunctions.exceptionToString(e));
            }
        }

        else if (headers.getDestination().contains(HealthCheckerMessageTopicConstants.TEST_CONFIG_REQUEST)) {
            try {
                WebSocketMessageDTO<HealthCheckerModuleConfigDTO> msg = this.mapper.readValue(payload.toString(), new TypeReference<WebSocketMessageDTO<HealthCheckerModuleConfigDTO>>() {});
                HealthCheckController c = new HealthCheckController(this.config);
                /*
                byte[] a = msg.getMessage().getAcceptableResponsePattern().getBytes();
                for(int i=0; i< a.length ; i++) {
                    System.out.print(a[i] + ", ");
                }
                 */
                if (StringUtils.hasText(this.destination) && !this.destination.equals(msg.getFrom())) {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.TEST_CONFIG_RESPONSE, msg.getFrom(), new Throwable("HealthChecker ocupado"));
                } else {
                    this.sendMessage(c.checkModule(msg.getMessage()), HealthCheckerMessageTopicConstants.TEST_CONFIG_RESPONSE);
                }

            } catch (Throwable e) {
                logger.error(UtilFunctions.exceptionToString(e));
            }
        }
        else if (headers.getDestination().contains(HealthCheckerMessageTopicConstants.SAVE_CONFIG_REQUEST)) {
            try {
                WebSocketMessageDTO<HealthCheckerConfigDTO> msg = this.mapper.readValue(payload.toString(), new TypeReference<WebSocketMessageDTO<HealthCheckerConfigDTO>>() {});
                if (StringUtils.hasText(this.destination) && !this.destination.equals(msg.getFrom())) {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE, msg.getFrom(), new Throwable("HealthChecker ocupado"));
                } else {
                    this.config = msg.getMessage();
                    UtilController.saveConfigFile(msg.getMessage());
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE);
                }

            } catch (Throwable e) {
                logger.error(UtilFunctions.exceptionToString(e));
                try {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE, e);
                } catch (Throwable ex) {
                    logger.error(UtilFunctions.exceptionToString(ex));
                }
            }
        }
        else if (headers.getDestination().contains(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START)) {
            try {
                WebSocketMessageDTO<Boolean> msg = this.mapper.readValue(payload.toString(), new TypeReference<WebSocketMessageDTO<Boolean>>() {});
                if (StringUtils.hasText(this.destination) && !this.destination.equals(msg.getFrom())) {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, msg.getFrom(), new Throwable("HealthChecker ocupado"));
                } else {
                    if (msg.getMessage() != null && msg.getMessage().booleanValue()) {
                        this.runSchedulerSI();
                    } else {
                        HealthCheckerSystemInfoDTO siDTO = readSystemInfo(true);
                        sendMessage(siDTO, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, null);
                    }
                }

            } catch (Throwable e) {
                logger.error(UtilFunctions.exceptionToString(e));
                try {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, e);
                } catch (Throwable ex) {
                    logger.error(UtilFunctions.exceptionToString(ex));
                }
            }
        }
        else if (headers.getDestination().contains(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP)) {
            try {
                WebSocketMessageDTO<Void> msg = this.mapper.readValue(payload.toString(), new TypeReference<WebSocketMessageDTO<Void>>() {});
                if (StringUtils.hasText(this.destination) && !this.destination.equals(msg.getFrom())) {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, msg.getFrom(), new Throwable("HealthChecker ocupado"));
                } else {
                    try {
                        this.stopScheduler(false);
                    } catch (Throwable ex) {

                    }
                    HealthCheckerSystemInfoDTO siDTO = readSystemInfo(false);
                    sendMessage(siDTO, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, null);
                }

            } catch (Throwable e) {
                logger.error(UtilFunctions.exceptionToString(e));
                try {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, e);
                } catch (Throwable ex) {
                    logger.error(UtilFunctions.exceptionToString(ex));
                }
            }
        }
        else if (headers.getDestination().contains(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST)) {
            try {
                WebSocketMessageDTO<String> msg = this.mapper.readValue(payload.toString(), new TypeReference<WebSocketMessageDTO<String>>() {});
                if (StringUtils.hasText(this.destination) && !this.destination.equals(msg.getFrom())) {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, msg.getFrom(), new Throwable("HealthChecker ocupado"));
                } else {
                    if (!this.scheduledResult.isDone() && !this.scheduledResult.isCancelled()) {
                        this.sendMessage(null, HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, msg.getFrom(), new Throwable("HealthChecker ocupado"));
                    } else {
                        this.commandLineController.executeCommand(msg.getMessage());
                    }

                }

            } catch (Throwable e) {
                logger.error(UtilFunctions.exceptionToString(e));
                try {
                    this.sendMessage(null, HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE, e);
                } catch (Throwable ex) {
                    logger.error(UtilFunctions.exceptionToString(ex));
                }
            }
        }
    }

    @Override
    public void handleException(StompSession session, @Nullable StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error(UtilFunctions.exceptionToString(exception));
//        if (!session.isConnected()) {
//            if (this.params.getActionType() == ActionType.init) {
//                this.stopScheduler(true);
//            } else if (this.params.getActionType() == ActionType.run) {
//                try {
//                    this.runScheduler();
//                } catch (Throwable e) {
//                    logger.error(UtilFunctions.exceptionToString(e));
//                    this.stopScheduler(true);
//                }
//            }
//        }
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        logger.error(UtilFunctions.exceptionToString(exception));
//        if (!session.isConnected()) {
//            if (this.params.getActionType() == ActionType.init) {
//                this.stopScheduler(true);
//            } else if (this.params.getActionType() == ActionType.run) {
//                try {
//                    this.runScheduler();
//                } catch (Throwable e) {
//                    logger.error(UtilFunctions.exceptionToString(e));
//                    this.stopScheduler(true);
//                }
//            }
//        }
    }

    @Override
    public void onInput(ConsoleResponseMessageDTO value) {
        try {
            this.sendMessage(value, HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_RESPONSE);

        } catch (Throwable ex) {
            logger.error(UtilFunctions.exceptionToString(ex));
        }
    }

    @Override
    public void onError(ConsoleResponseMessageDTO value) {
        try {
            this.sendMessage(value, HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_RESPONSE);

        } catch (Throwable ex) {
            logger.error(UtilFunctions.exceptionToString(ex));
        }
    }

    @Override
    public void onFinish() {

    }

    private class SystemInfoRuntime implements Callable<HealthCheckerSystemInfoDTO> {
        SystemInfo si;
        HealthCheckerSystemInfoDTO dto;
        private boolean forceUpdate = false;

        public SystemInfoRuntime() {
            this.si = new SystemInfo();

        }
        @Override
        public HealthCheckerSystemInfoDTO call() throws Exception {
            if (this.dto == null) {
                this.dto = new HealthCheckerSystemInfoDTO();
                this.forceUpdate = true;
            }
            if (this.forceUpdate) {
                this.getHardware(si.getHardware());
                this.getOS(si.getOperatingSystem(), si.getHardware());
                this.dto.setLastUpdate(Calendar.getInstance());
                this.forceUpdate = false;
            }
            return this.dto;
        }

        private void getHardware(HardwareAbstractionLayer hal) throws Exception {
            HealthCheckerSystemInfoDTO.HardwareDTO d = this.dto.getHardware();
            if (d == null) {
                d = new HealthCheckerSystemInfoDTO.HardwareDTO();
                this.dto.setHardware(d);
            }
            d.setComputerSystem(getHardwareComputerSystem(hal));
            d.setProcessor(getHardwareProcessor(hal));
            d.setMemory(getHardwareMemory(hal));
            d.setDiskStores(getHardwareDisks(hal));
            d.setLogicalVolumeGroups(getHardwareLogivalVolumes(hal));
            d.setNetworkIFs(getHardwareNetworkIFs(hal));
        }

        private HealthCheckerSystemInfoDTO.ComputerSystemDTO getHardwareComputerSystem(HardwareAbstractionLayer hal) throws Exception {
            HealthCheckerSystemInfoDTO.ComputerSystemDTO d = this.dto.getHardware().getComputerSystem();
            if (d == null) {
                d = mapper.readValue(mapper.writeValueAsString(hal.getComputerSystem()), HealthCheckerSystemInfoDTO.ComputerSystemDTO.class);
            }
            return d;
        }

        private HealthCheckerSystemInfoDTO.CentralProcessorDTO getHardwareProcessor(HardwareAbstractionLayer hal) throws Exception {
            HealthCheckerSystemInfoDTO.CentralProcessorDTO d = this.dto.getHardware().getProcessor();
            CentralProcessor processor = hal.getProcessor();
            if (d == null) {
                d = new HealthCheckerSystemInfoDTO.CentralProcessorDTO();
                d.setPrevTicks(processor.getSystemCpuLoadTicks());
                d.setProcessorIdentifier(mapper.readValue(mapper.writeValueAsString(processor.getProcessorIdentifier()), HealthCheckerSystemInfoDTO.ProcessorIdentifierDTO.class));
            }
            BeanUtils.copyProperties(processor, d);
            long[] prevTicks = d.getPrevTicks();
            d.setCpuLoad(processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100);
            d.setPrevTicks(processor.getSystemCpuLoadTicks());
            return d;
        }

        private HealthCheckerSystemInfoDTO.GlobalMemoryDTO getHardwareMemory(HardwareAbstractionLayer hal) throws Exception {
            HealthCheckerSystemInfoDTO.GlobalMemoryDTO d = mapper.readValue(mapper.writeValueAsString(hal.getMemory()), HealthCheckerSystemInfoDTO.GlobalMemoryDTO.class);
            if (MainController.isLinux) {
                //d.setAvailable(d.getTotal() - getUsedMemoryFromLinux());
            }
            return d;
        }

        private List<HealthCheckerSystemInfoDTO.HWDiskStoreDTO> getHardwareDisks(HardwareAbstractionLayer hal) throws Exception {
            List<HealthCheckerSystemInfoDTO.HWDiskStoreDTO> d = mapper.readValue(mapper.writeValueAsString(hal.getDiskStores()), new TypeReference<List<HealthCheckerSystemInfoDTO.HWDiskStoreDTO>>() {});
            return d;
        }

        private List<HealthCheckerSystemInfoDTO.LogicalVolumeGroupDTO> getHardwareLogivalVolumes(HardwareAbstractionLayer hal) throws Exception {
            List<HealthCheckerSystemInfoDTO.LogicalVolumeGroupDTO> d = mapper.readValue(mapper.writeValueAsString(hal.getLogicalVolumeGroups()), new TypeReference<List<HealthCheckerSystemInfoDTO.LogicalVolumeGroupDTO>>() {});
            return d;
        }

        private List<HealthCheckerSystemInfoDTO.NetworkIFDTO> getHardwareNetworkIFs(HardwareAbstractionLayer hal) throws Exception {
            List<HealthCheckerSystemInfoDTO.NetworkIFDTO> d = mapper.readValue(mapper.writeValueAsString(hal.getNetworkIFs()), new TypeReference<List<HealthCheckerSystemInfoDTO.NetworkIFDTO>>() {});
            return d;
        }

        private void getOS(OperatingSystem os, HardwareAbstractionLayer hal) throws Exception {
            HealthCheckerSystemInfoDTO.OperatingSystemDTO d = this.dto.getOperatingSystem();
            if (d == null) {
                d = new HealthCheckerSystemInfoDTO.OperatingSystemDTO();
                this.dto.setOperatingSystem(d);
            }
            BeanUtils.copyProperties(os, d);
            d.setVersionInfo(getOSVersionInfo(os));
            d.setFileSystem(getOSFileSystem(os));
            d.setNetworkParams(getOSNetworkParams(os));
            d.setServices(getOSServices(os));
            d.setInternetProtocolStats(getOSInternetProtocolStats(os));
            d.setSessions(getOSSessions(os));
            d.setCurrentProcess(getCurrentProcess(os));
            d.setProcesses(this.getOSProcess(os, hal));

        }
        private HealthCheckerSystemInfoDTO.OSVersionInfoDTO getOSVersionInfo(OperatingSystem os) throws Exception {
            HealthCheckerSystemInfoDTO.OSVersionInfoDTO d = new HealthCheckerSystemInfoDTO.OSVersionInfoDTO();
            BeanUtils.copyProperties(os.getVersionInfo(), d);
            return d;
        }

        private HealthCheckerSystemInfoDTO.FileSystemDTO getOSFileSystem(OperatingSystem os) throws Exception {
            HealthCheckerSystemInfoDTO.FileSystemDTO d = new HealthCheckerSystemInfoDTO.FileSystemDTO();
            FileSystem fs = os.getFileSystem();
            BeanUtils.copyProperties(fs, d);
            d.setFileStores(getOSFileSystemFileStore(fs));
            return d;
        }

        private List<HealthCheckerSystemInfoDTO.FileStoreDTO> getOSFileSystemFileStore(FileSystem os) throws Exception {
            List<HealthCheckerSystemInfoDTO.FileStoreDTO> d = new ArrayList<>();
            for(OSFileStore fs : os.getFileStores(true)) {
                HealthCheckerSystemInfoDTO.FileStoreDTO x = new HealthCheckerSystemInfoDTO.FileStoreDTO();
                BeanUtils.copyProperties(fs, x);
                d.add(x);
            }
            return d;
        }

        private HealthCheckerSystemInfoDTO.NetworkParamsDTO getOSNetworkParams(OperatingSystem os) throws Exception {
            HealthCheckerSystemInfoDTO.NetworkParamsDTO d = mapper.readValue(mapper.writeValueAsString(os.getNetworkParams()), HealthCheckerSystemInfoDTO.NetworkParamsDTO.class);
            return d;
        }

        private List<HealthCheckerSystemInfoDTO.OSServiceDTO> getOSServices(OperatingSystem os) throws Exception {
            List<HealthCheckerSystemInfoDTO.OSServiceDTO> d = mapper.readValue(mapper.writeValueAsString(os.getServices()), new TypeReference<List<HealthCheckerSystemInfoDTO.OSServiceDTO>>() {});
            return d;
        }

        private HealthCheckerSystemInfoDTO.InternetProtocolStatsDTO getOSInternetProtocolStats(OperatingSystem os) throws Exception {
            HealthCheckerSystemInfoDTO.InternetProtocolStatsDTO d = mapper.readValue(mapper.writeValueAsString(os.getInternetProtocolStats()), HealthCheckerSystemInfoDTO.InternetProtocolStatsDTO.class);
            return d;
        }

        private List<HealthCheckerSystemInfoDTO.OSSessionDTO> getOSSessions(OperatingSystem os) throws Exception {
            List<HealthCheckerSystemInfoDTO.OSSessionDTO> d = mapper.readValue(mapper.writeValueAsString(os.getSessions()), new TypeReference<List<HealthCheckerSystemInfoDTO.OSSessionDTO>>() {});
            return d;
        }

        private HealthCheckerSystemInfoDTO.OSProcessDTO getCurrentProcess (OperatingSystem os) throws Exception {
            OSProcess currentProcess = os.getCurrentProcess();
            HealthCheckerSystemInfoDTO.OSProcessDTO processDTO = new HealthCheckerSystemInfoDTO.OSProcessDTO();
            BeanUtils.copyProperties(currentProcess, processDTO);
            return processDTO;
        }

        private List<HealthCheckerSystemInfoDTO.OSProcessDTO> getOSProcess(OperatingSystem os, HardwareAbstractionLayer hal) throws Exception {
            GlobalMemory memory = hal.getMemory();

            List<HealthCheckerSystemInfoDTO.OSProcessDTO> d = this.dto.getOperatingSystem().getProcesses();
            if (d == null) {
                d = new ArrayList<>();
            }
            long uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().getTimeInMillis());

            List<OSProcess> procs = os.getProcesses(OperatingSystem.ProcessFiltering.ALL_PROCESSES, OperatingSystem.ProcessSorting.CPU_DESC, this.dto.getOperatingSystem().getProcessCount());

            int cpuNumber = this.dto.getHardware().getProcessor().getLogicalProcessorCount();
            /*
            List<OSProcess> procs = os.getProcesses((p) -> {
                return p.getProcessID() > 0;
            }, OperatingSystem.ProcessSorting.CPU_DESC, this.dto.getOperatingSystem().getProcessCount());
             */
            for (int i = 0; i < procs.size(); i++) {
                OSProcess p = procs.get(i);
                int processId = p.getProcessID();
                if (p == null) {
                    continue;
                }
                HealthCheckerSystemInfoDTO.OSProcessDTO processDTO = d.stream().filter(x -> x.getProcessID() == processId).findFirst().orElse(null);
                if (processDTO == null) {
                    processDTO = new HealthCheckerSystemInfoDTO.OSProcessDTO();
                    processDTO.setPreviousProcess(p);
                    d.add(processDTO);
                }

                double cpu = 100d * p.getProcessCpuLoadBetweenTicks(processDTO.getPreviousProcess()) / (os.getFamily().equalsIgnoreCase("windows")?cpuNumber:1 );
                double mem = 100d * p.getResidentSetSize() / memory.getTotal();
                BeanUtils.copyProperties(p, processDTO);
                processDTO.setMemoryUsagePercent(mem);
                processDTO.setCpuUsagePercent(cpu);
                processDTO.setUpTime(TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().getTimeInMillis()));
                processDTO.setPreviousProcess(p);
            }
            d.removeAll(d.stream().filter(x -> x.getUpTime() < uptimeSeconds).collect(Collectors.toList()));
            return d;
        }

        public boolean isForceUpdate() {
            return forceUpdate;
        }

        public void setForceUpdate(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
        }

        private long getCacheMemoryFromLinux() {
            try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    //logger.info("Lendo Cache " + line);
                    if (line.toUpperCase().startsWith("CACHED:")) {
                        String[] parts = line.split("\\s+");
                        long total = Long.parseLong(parts[1]) * 1024;
                        //logger.info("total " + total);
                        return total;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        private long getUsedMemoryFromLinux() {
            try {
                // Run the command
                ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "free | awk '/Mem:/ {print $3}'");
                Process process = processBuilder.start();

                // Read the output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null) {
                        long usedMemory = Long.parseLong(line.trim()) * 1024;
                        return usedMemory;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public Calendar getLastUpdate() {
            if (this.dto == null) {
                return null;
            }
            return this.dto.getLastUpdate();
        }

    }

}
