package br.com.evolui.portalevolui.web.rest.dto.github;

import java.util.*;
import java.util.stream.Collectors;

public class GithubCommitStatisticDTO {
    private Integer total;
    private LinkedHashMap<Calendar, Integer> perDay;
    private LinkedHashMap<String, CommitAuthorCount> perAuthor;

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public LinkedHashMap<Calendar, Integer> getPerDay() {
        return perDay;
    }

    public void setPerDay(LinkedHashMap<Calendar, Integer> perDay) {
        this.perDay = perDay;
    }

    public LinkedHashMap<String, CommitAuthorCount> getPerAuthor() {
        return perAuthor;
    }

    public void setPerAuthor(LinkedHashMap<String, CommitAuthorCount> perAuthor) {
        this.perAuthor = perAuthor;
    }

    public static GithubCommitStatisticDTO parseFromHistory(List<GithubCommitHistoryDTO> history) {
        GithubCommitStatisticDTO dto = new GithubCommitStatisticDTO();
        dto.perAuthor = new LinkedHashMap<>();
        dto.perDay = new LinkedHashMap<>();
        dto.setTotal(history.size());
        history.forEach(x -> {
            CommitAuthorCount author = dto.perAuthor.entrySet().stream().filter(y -> y.getKey().equals(x.getCommit().getAuthor().getEmail())).findFirst().map(y -> y.getValue()).orElse(null);
            if (author != null) {
                author.setCount(author.getCount() + 1);
            }
            else {
                author = new CommitAuthorCount();
                author.setAuthor(x.getCommit().getAuthor());
                author.setCount(1);
                dto.perAuthor.put(x.getCommit().getAuthor().getEmail(), author);
            }
            Calendar commitDate = (Calendar) x.getCommit().getAuthor().getDate().clone();
            commitDate.set(Calendar.HOUR_OF_DAY, 0);
            commitDate.set(Calendar.MINUTE, 0);
            commitDate.set(Calendar.SECOND, 0);
            commitDate.set(Calendar.MILLISECOND, 0);

            Map.Entry<Calendar, Integer> day = dto.perDay.entrySet().stream().filter(y -> y.getKey().compareTo(commitDate) == 0).findFirst().orElse(null);
            if (day != null) {
                day.setValue(day.getValue() + 1);
            }
            else {
                dto.perDay.put(commitDate, 1);
            }
        });
        dto.perDay = dto.perDay.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        dto.perAuthor = dto.perAuthor.entrySet().stream().sorted(Collections.reverseOrder(Comparator.comparingInt(o -> o.getValue().getCount())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        return dto;
    }

    public static class CommitAuthorCount {
        private GithubCommitHistoryDTO.Author author;
        private Integer count;

        public GithubCommitHistoryDTO.Author getAuthor() {
            return author;
        }

        public void setAuthor(GithubCommitHistoryDTO.Author author) {
            this.author = author;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }
}
