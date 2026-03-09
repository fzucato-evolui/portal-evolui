package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.CICDReportStatusTypeEnum;
import br.com.evolui.portalevolui.web.beans.enums.GithubActionConclusionEnum;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.HealthCheckerEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.GoogleConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.AtualizacaoVersaoEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.BackupRestoreEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.CICDEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.GeracaoVersaoEmailNotificationDTO;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.chat.v1.HangoutsChat;
import com.google.api.services.chat.v1.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoogleService {
    private GoogleConfigDTO config;

    @Autowired
    private SystemConfigRepository configRepository;

    public List<Space> getSpaces() throws Exception {
        HangoutsChat client = this.getChatClient();
        List<Space> spaces = new ArrayList<>();
        String nextToken = null;
        while (true) {
            ListSpacesResponse ls;
            if(StringUtils.hasText(nextToken)) {
                ls = client.spaces().list().execute().setNextPageToken(nextToken);
            } else {
                ls = client.spaces().list().execute();
            }
            if (ls.getSpaces() != null && !ls.getSpaces().isEmpty()) {
                List<Space> filtered = ls.getSpaces().stream().filter(x -> StringUtils.hasText(x.getDisplayName())).collect(Collectors.toList());
                if (filtered != null && !filtered.isEmpty())
                    spaces.addAll(filtered);
            }

            nextToken = ls.getNextPageToken();
            if(!StringUtils.hasText(nextToken)) {
                break;
            }
        }
        return spaces;

    }

    public void sendVersionSync(GeracaoVersaoEmailNotificationDTO dto) throws Exception {
        this.sendVersion(dto);
    }

    @Async
    public void sendVersionAsync(GeracaoVersaoEmailNotificationDTO dto) throws Exception {
        this.sendVersion(dto);
    }

    public void sendUpdateSync(AtualizacaoVersaoEmailNotificationDTO dto) throws Exception {
        this.sendUpdate(dto);
    }

    @Async
    public void sendUpdateAsync(AtualizacaoVersaoEmailNotificationDTO dto) throws Exception {
        this.sendUpdate(dto);
    }

    public void sendCICDSync(CICDEmailNotificationDTO dto) throws Exception {
        this.sendCICD(dto);
    }

    @Async
    public void sendCICDAsync(CICDEmailNotificationDTO dto) throws Exception {
        this.sendCICD(dto);
    }

    public void sendHealthCheckerSync(HealthCheckerEmailNotificationDTO dto) throws Exception {
        this.sendHealthChecker(dto);
    }

    @Async
    public void sendHealthCheckerAsync(HealthCheckerEmailNotificationDTO dto) throws Exception {
        this.sendHealthChecker(dto);
    }

    public void sendBackupRestoreSync(BackupRestoreEmailNotificationDTO dto) throws Exception {
        this.sendBackupRestore(dto);
    }

    @Async
    public void sendBackupRestoreAsync(BackupRestoreEmailNotificationDTO dto) throws Exception {
        this.sendBackupRestore(dto);
    }

    private void sendVersion(GeracaoVersaoEmailNotificationDTO dto) throws Exception {
        HangoutsChat client = this.getChatClient();
        Card c = new Card();
        CardHeader header = new CardHeader();
        header.setTitle("Geracão de Versão " + dto.getSubject());
        c.setHeader(header);
        List<Section> sessions = new ArrayList<>();
        {
            Section s = new Section();
            s.setHeader(dto.getTitle());
            {
                List<WidgetMarkup> widgets = new ArrayList<>();
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Responsável:</b> " + dto.getAuthor());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Início:</b> " + dto.getRequestDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Conclusão:</b> " + dto.getConclusionDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<a href=\"" + dto.getLink() + "\">Saiba mais</a>");
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                s.setWidgets(widgets);
            }
            sessions.add(s);

        }
        if (dto.isSuccess()){
            Section s = new Section();
            s.setHeader("Módulos da Versão");
            List<WidgetMarkup> widgets = new ArrayList<>();
            for (GeracaoVersaoEmailNotificationDTO.ModulesDTO m : dto.getModules()) {

                WidgetMarkup w = new WidgetMarkup();
                TextParagraph t = new TextParagraph();
                t.setText("<b>" + m.getName() + ":</b> " + m.getVersion());
                w.setTextParagraph(t);
                widgets.add(w);

            }
            s.setWidgets(widgets);
            sessions.add(s);

        }
        c.setSections(sessions);
        // Create a Chat message.
        //Message message = new Message().setText("Hello, world!");
        Message message = new Message().setCards(Arrays.asList(c));
        for (String destination : dto.getDestinations()) {
            String spaceName = destination;
            client.spaces().messages().create(spaceName, message).execute();
        }
    }

    private void sendUpdate(AtualizacaoVersaoEmailNotificationDTO dto) throws Exception {
        HangoutsChat client = this.getChatClient();
        Card c = new Card();
        CardHeader header = new CardHeader();
        header.setTitle("Atualização de Versão " + dto.getSubject());
        c.setHeader(header);
        List<Section> sessions = new ArrayList<>();
        {
            Section s = new Section();
            s.setHeader(dto.getTitle());
            {
                List<WidgetMarkup> widgets = new ArrayList<>();
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Responsável:</b> " + dto.getAuthor());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Início:</b> " + dto.getRequestDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Conclusão:</b> " + dto.getConclusionDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<a href=\"" + dto.getLink() + "\">Saiba mais</a>");
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                s.setWidgets(widgets);
            }
            sessions.add(s);

        }
        if (dto.getConclusion() == GithubActionConclusionEnum.success || dto.getConclusion() == GithubActionConclusionEnum.warning){
            Section s = new Section();
            s.setHeader("Módulos da Versão");
            List<WidgetMarkup> widgets = new ArrayList<>();
            for (AtualizacaoVersaoEmailNotificationDTO.ModulesDTO m : dto.getModules()) {

                WidgetMarkup w = new WidgetMarkup();
                TextParagraph t = new TextParagraph();
                t.setText("<b>" + m.getName() + ":</b> " + m.getVersion());
                w.setTextParagraph(t);
                widgets.add(w);

            }
            s.setWidgets(widgets);
            sessions.add(s);

        }
        c.setSections(sessions);
        // Create a Chat message.
        //Message message = new Message().setText("Hello, world!");
        Message message = new Message().setCards(Arrays.asList(c));
        for (String destination : dto.getDestinations()) {
            String spaceName = destination;
            client.spaces().messages().create(spaceName, message).execute();
        }
    }

    private void sendCICD(CICDEmailNotificationDTO dto) throws Exception {
        HangoutsChat client = this.getChatClient();
        Card c = new Card();
        CardHeader header = new CardHeader();
        header.setTitle(dto.getSubject());
        c.setHeader(header);
        List<Section> sessions = new ArrayList<>();
        {
            Section s = new Section();
            s.setHeader(dto.getTitle());
            {
                List<WidgetMarkup> widgets = new ArrayList<>();

                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Início:</b> " + dto.getRequestDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Conclusão:</b> " + dto.getConclusionDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<a href=\"" + dto.getLink() + "\">Saiba mais</a>");
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                s.setWidgets(widgets);
            }
            sessions.add(s);

        }
        Section s = new Section();
        s.setHeader("Módulos Testados");
        List<WidgetMarkup> widgets = new ArrayList<>();
        for (CICDEmailNotificationDTO.ModulesDTO m : dto.getModules()) {

            WidgetMarkup w = new WidgetMarkup();
            TextParagraph t = new TextParagraph();
            if (!m.getStatus().equals(CICDReportStatusTypeEnum.FAILURE.value())) {
                t.setText("<b>" + m.getName() +"<font color=\"#00FF00\">"+ m.getStatus() + "</font></b> ");
            }
            else {
                t.setText("<b>" + m.getName() +"<font color=\"#FF0000\">"+ m.getStatus() + "</font></b> ");
            }
            w.setTextParagraph(t);
            widgets.add(w);

        }
        s.setWidgets(widgets);
        sessions.add(s);
        c.setSections(sessions);
        // Create a Chat message.
        //Message message = new Message().setText("Hello, world!");
        Message message = new Message().setCards(Arrays.asList(c));
        for (String destination : dto.getDestinations()) {
            String spaceName = destination;
            client.spaces().messages().create(spaceName, message).execute();
        }
    }

    private void sendHealthChecker(HealthCheckerEmailNotificationDTO dto) throws Exception {
        HangoutsChat client = this.getChatClient();
        Card c = new Card();
        CardHeader header = new CardHeader();
        header.setTitle(dto.getSubject());
        c.setHeader(header);
        List<Section> sessions = new ArrayList<>();
        {
            Section s = new Section();
            s.setHeader(dto.getTitle());
            {
                List<WidgetMarkup> widgets = new ArrayList<>();
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    if (dto.isHealth()) {
                        t.setText("<b><font color=\"#00FF00\">Saudável</font></b> ");
                    }
                    else {
                        t.setText("<b><font color=\"#FF0000\">NÃO Saudável</font></b> ");
                    }

                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Responsável:</b> " + dto.getAuthor());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Atualização:</b> " + dto.getRequestDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Saudável Em:</b> " + dto.getLastHealthDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<a href=\"" + dto.getLink() + "\">Saiba mais</a>");
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                s.setWidgets(widgets);
            }
            sessions.add(s);

        }

        if (dto.getAlerts() != null && !dto.getAlerts().isEmpty()){
            Section s = new Section();
            s.setHeader("Alertas");
            List<WidgetMarkup> widgets = new ArrayList<>();
            for (HealthCheckerEmailNotificationDTO.AlertDTO m : dto.getAlerts()) {

                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>" + m.getAlertType() + "</b>");
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    if (m.isHealth()) {
                        t.setText("<b><font color=\"#00FF00\">" + m.getMessage() + "</font></b> ");
                    }
                    else {
                        t.setText("<b><font color=\"#FF0000\">"+ m.getMessage() +"</font></b> ");
                    }
                    w.setTextParagraph(t);
                    widgets.add(w);
                }

            }
            s.setWidgets(widgets);
            sessions.add(s);

        }

        if (dto.getModules() != null && !dto.getModules().isEmpty()){
            Section s = new Section();
            s.setHeader("Módulos");
            List<WidgetMarkup> widgets = new ArrayList<>();
            for (HealthCheckerEmailNotificationDTO.ModulesDTO m : dto.getModules()) {

                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>" + m.getName() + ": </b> Saudável em " + m.getLastHealthDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    if (m.isHealth()) {
                        t.setText("<b><font color=\"#00FF00\">" + m.getMessage() + "</font></b> ");
                    }
                    else {
                        t.setText("<b><font color=\"#FF0000\">"+ m.getMessage() +"</font></b> ");
                    }
                    w.setTextParagraph(t);
                    widgets.add(w);
                }

            }
            s.setWidgets(widgets);
            sessions.add(s);

        }
        c.setSections(sessions);
        // Create a Chat message.
        //Message message = new Message().setText("Hello, world!");
        Message message = new Message().setCards(Arrays.asList(c));
        for (String destination : dto.getDestinations()) {
            String spaceName = destination;
            client.spaces().messages().create(spaceName, message).execute();
        }
    }

    private void sendBackupRestore(BackupRestoreEmailNotificationDTO dto) throws Exception {
        HangoutsChat client = this.getChatClient();
        Card c = new Card();
        CardHeader header = new CardHeader();
        header.setTitle("Backup/Restore " + dto.getSubject());
        c.setHeader(header);
        List<Section> sessions = new ArrayList<>();
        {
            Section s = new Section();
            s.setHeader(dto.getTitle());
            {
                List<WidgetMarkup> widgets = new ArrayList<>();
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Responsável:</b> " + dto.getAuthor());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Início:</b> " + dto.getRequestDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                {
                    WidgetMarkup w = new WidgetMarkup();
                    TextParagraph t = new TextParagraph();
                    t.setText("<b>Data Conclusão:</b> " + dto.getConclusionDate());
                    w.setTextParagraph(t);
                    widgets.add(w);
                }
                s.setWidgets(widgets);
            }
            sessions.add(s);

        }
        c.setSections(sessions);
        // Create a Chat message.
        //Message message = new Message().setText("Hello, world!");
        Message message = new Message().setCards(Arrays.asList(c));
        for (String destination : dto.getDestinations()) {
            String spaceName = destination;
            client.spaces().messages().create(spaceName, message).execute();
        }
    }

    //@Override
    public boolean initialize(Object... param) {
        return this.getConfig() != null;
    }

    //@Override
    public void refresh() {
        this.config = null;
    }

    //@Override
    public SystemConfigBean getSystemConfig() {
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.GOOGLE).orElse(null);
    }

    public GoogleConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (GoogleConfigDTO) c.getConfig();
            }
        }
        return config;
    }

    public void setConfig(GoogleConfigDTO dto) {
        this.config = dto;
    }

    private HangoutsChat getChatClient() throws Exception {
        String CHAT_SCOPE = "https://www.googleapis.com/auth/chat.bot";
        GoogleCredentials credentials = this.getCredentials();

        credentials = credentials.createScoped(CHAT_SCOPE);

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
        HangoutsChat chatService = new HangoutsChat.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Portal Evolui")
                .build();
        return chatService;
    }

    private GoogleCredentials getCredentials() throws Exception {
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(this.getConfig().getServiceAccount().toStream());
        return credentials;
    }

}
