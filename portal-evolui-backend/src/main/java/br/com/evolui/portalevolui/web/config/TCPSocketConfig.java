package br.com.evolui.portalevolui.web.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

//@Configuration
public class TCPSocketConfig {
    @Value("${wss.port}")
    private int WSS_PORT;


    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new     com.corundumstudio.socketio.Configuration();
        config.setPingTimeout(6000000);
        //config.setHostname(WSS_HOST);
        config.setPort(WSS_PORT);
        //This is the authentication h
        config.setAuthorizationListener(handshakeData -> {
            return true;
        });
        final SocketIOServer server = new SocketIOServer(config);
        return server;
    }

    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
        return new SpringAnnotationScanner(socketServer);
    }

}