package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.SystemConfigBean;
import br.com.evolui.portalevolui.web.beans.enums.SystemConfigTypeEnum;
import br.com.evolui.portalevolui.web.repository.SystemConfigRepository;
import br.com.evolui.portalevolui.web.rest.dto.HealthCheckerEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.config.SMTPConfigDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.AtualizacaoVersaoEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.BackupRestoreEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.CICDEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.dto.version.GeracaoVersaoEmailNotificationDTO;
import br.com.evolui.portalevolui.web.rest.intefaces.ISystemConfigService;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

@Service
public class SMTPService implements ISystemConfigService {
    private SMTPConfigDTO config;
    private JavaMailSender javaMailSender;
    private TemplateEngine templateEngine;

    @Autowired
    private SystemConfigRepository configRepository;


    @Transactional
    public void sendTest(String destination) throws Exception {

        this.setSender();
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        message.setTo(destination);

        message.setFrom(this.getConfig().getSenderEmail(), this.getConfig().getSenderName());
        message.setSubject("Teste de envio de email");


        String content = "Teste de envio de email feito pelo Portal Evolui";
        message.setText(content, true);

        javaMailSender.send(mimeMessage);
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

        this.setSender();
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        message.setTo(dto.getDestinations().toArray(new String[0]));


        message.setFrom(getConfig().getSenderEmail(), getConfig().getSenderName());
        message.setSubject(dto.getSubject());


        String content = this.getBodyVersionMessage(dto);
        message.setText(content, true);

        javaMailSender.send(mimeMessage);
    }

    private void sendUpdate(AtualizacaoVersaoEmailNotificationDTO dto) throws Exception {

        this.setSender();
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        message.setTo(dto.getDestinations().toArray(new String[0]));


        message.setFrom(getConfig().getSenderEmail(), getConfig().getSenderName());
        message.setSubject(dto.getSubject());


        String content = this.getBodyUpdateMessage(dto);
        message.setText(content, true);

        javaMailSender.send(mimeMessage);
    }

    private void sendCICD(CICDEmailNotificationDTO dto) throws Exception {

        this.setSender();
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        message.setTo(dto.getDestinations().toArray(new String[0]));


        message.setFrom(getConfig().getSenderEmail(), getConfig().getSenderName());
        message.setSubject(dto.getSubject());


        String content = this.getBodyCICDMessage(dto);
        message.setText(content, true);

        javaMailSender.send(mimeMessage);
    }

    private void sendHealthChecker(HealthCheckerEmailNotificationDTO dto) throws Exception {

        this.setSender();
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        message.setTo(dto.getDestinations().toArray(new String[0]));


        message.setFrom(getConfig().getSenderEmail(), getConfig().getSenderName());
        message.setSubject(dto.getSubject());


        String content = this.getBodyHealthCheckerMessage(dto);
        message.setText(content, true);

        javaMailSender.send(mimeMessage);
    }

    private void sendBackupRestore(BackupRestoreEmailNotificationDTO dto) throws Exception {
        this.setSender();
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        message.setTo(dto.getDestinations().toArray(new String[0]));


        message.setFrom(getConfig().getSenderEmail(), getConfig().getSenderName());
        message.setSubject(dto.getSubject());


        String content = this.getBodyBackupRestoreMessage(dto);
        message.setText(content, true);

        javaMailSender.send(mimeMessage);
    }

    @Override
    public boolean initialize(Object... param) {
        return this.getConfig() != null;
    }

    @Override
    public void dispose() {
        this.config = null;
        this.javaMailSender = null;
    }

    @Override
    public SystemConfigBean getSystemConfig() {
        return this.configRepository.findByConfigType(SystemConfigTypeEnum.SMTP).orElse(null);
    }

    public SMTPConfigDTO getConfig() {
        if (this.config == null) {
            SystemConfigBean c = this.getSystemConfig();
            if (c != null) {
                this.config = (SMTPConfigDTO) c.getConfig();
            }
        }
        return config;
    }

    public void setConfig(SMTPConfigDTO dto) {
        this.config = dto;
    }

    private void setSender() throws Exception {

        if (this.javaMailSender == null) {

            Properties prop = new Properties();
            prop.put("mail.smtp.auth", true);
            prop.put("mail.smtp.starttls.enable", this.getConfig().getSsl());
            prop.put("mail.smtp.host", this.getConfig().getServer());
            prop.put("mail.smtp.port", this.getConfig().getPort());
            prop.put("mail.smtp.ssl.trust", this.getConfig().getServer());
            final String user = this.getConfig().getUser();
            final String password = this.getConfig().getPassword();

            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            });

            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setSession(session);
            mailSender.setDefaultEncoding("UTF-8");

            this.javaMailSender = mailSender;

        }

    }

    private String getBodyVersionMessage(GeracaoVersaoEmailNotificationDTO dto) {

        Locale locale = Locale.forLanguageTag("pt-BR");
        Context context = new Context(locale);
        context.setVariable("dto", dto);

        String template = "email/geracaoVersao";
        this.templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(1));
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine.process(template, context);
    }

    private String getBodyUpdateMessage(AtualizacaoVersaoEmailNotificationDTO dto) {

        Locale locale = Locale.forLanguageTag("pt-BR");
        Context context = new Context(locale);
        context.setVariable("dto", dto);

        String template = "email/atualizacaoVersao";
        this.templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(1));
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine.process(template, context);
    }

    private String getBodyCICDMessage(CICDEmailNotificationDTO dto) {

        Locale locale = Locale.forLanguageTag("pt-BR");
        Context context = new Context(locale);
        context.setVariable("dto", dto);

        String template = "email/cicd";
        this.templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(1));
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine.process(template, context);
    }

    private String getBodyHealthCheckerMessage(HealthCheckerEmailNotificationDTO dto) {

        Locale locale = Locale.forLanguageTag("pt-BR");
        Context context = new Context(locale);
        context.setVariable("dto", dto);

        String template = "email/healthChecker";
        this.templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(1));
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine.process(template, context);
    }

    private String getBodyBackupRestoreMessage(BackupRestoreEmailNotificationDTO dto) {

        Locale locale = Locale.forLanguageTag("pt-BR");
        Context context = new Context(locale);
        context.setVariable("dto", dto);

        String template = "email/backupRestore";
        this.templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(1));
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine.process(template, context);
    }

}
