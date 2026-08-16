package br.com.evolui.portalevolui.web.rest.dto.version;

import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerDTO;
import br.com.evolui.portalevolui.web.rest.dto.github.GithubRunnerLabelDTO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class RunnersOfflineEmailNotificationDTO {
    private String title;
    private String subject;
    private String checkedAt;
    private List<String> destinations;
    private List<RunnerDTO> runners;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(String checkedAt) {
        this.checkedAt = checkedAt;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<String> destinations) {
        this.destinations = destinations;
    }

    public void addDestination(String destination) {
        if (this.destinations == null) {
            this.destinations = new ArrayList<>();
        }
        this.destinations.add(destination);
    }

    public List<RunnerDTO> getRunners() {
        return runners;
    }

    public void setRunners(List<RunnerDTO> runners) {
        this.runners = runners;
    }

    public static RunnersOfflineEmailNotificationDTO fromList(List<GithubRunnerDTO> offlineRunners, List<String> destinations) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        RunnersOfflineEmailNotificationDTO dto = new RunnersOfflineEmailNotificationDTO();
        dto.setSubject("Runners self-hosted offline");
        dto.setTitle(String.format("%d runner(s) self-hosted offline", offlineRunners.size()));
        dto.setCheckedAt(sdf.format(Calendar.getInstance().getTime()));
        dto.setDestinations(destinations);
        dto.setRunners(offlineRunners.stream().map(RunnerDTO::fromBean).collect(Collectors.toList()));
        return dto;
    }

    public static class RunnerDTO {
        private String name;
        private String os;
        private String status;
        private String labels;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLabels() {
            return labels;
        }

        public void setLabels(String labels) {
            this.labels = labels;
        }

        public static RunnerDTO fromBean(GithubRunnerDTO bean) {
            RunnerDTO dto = new RunnerDTO();
            dto.setName(bean.getName());
            dto.setOs(bean.getOs());
            dto.setStatus(bean.getStatus());
            if (bean.getLabels() != null && !bean.getLabels().isEmpty()) {
                dto.setLabels(bean.getLabels().stream().map(GithubRunnerLabelDTO::getName).collect(Collectors.joining(", ")));
            }
            return dto;
        }
    }
}
