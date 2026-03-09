package br.com.evolui.portalevolui.web.rest.dto.aws;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupRestoreRDSDTO {
    private static final int MAX_LOGS = 100;
    private static final int MAX_LISTENERS_LOGS = 2000;
    private final ActionRDSBean bean;
    private AtomicBoolean canceled = new AtomicBoolean(false);
    private Throwable error;
    private String warning;
    private final LinkedBlockingQueue<BackupRestoreRDSStatusDTO> lastLogs = new LinkedBlockingQueue<>(MAX_LOGS);
    private final ConcurrentHashMap<Long, LinkedBlockingQueue<BackupRestoreRDSStatusDTO>> listeners = new ConcurrentHashMap<>(MAX_LISTENERS_LOGS);

    public BackupRestoreRDSDTO(ActionRDSBean bean) {
        this.bean = bean;
    }

    public ActionRDSBean getBean() {
        return bean;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public boolean isCanceled() {
        return canceled.get();
    }

    public void cancel() {
        this.canceled.set(true);
    }

    public void registerListener() {
        long threadId = Thread.currentThread().getId();

        listeners.computeIfAbsent(threadId, k -> {
            LinkedBlockingQueue<BackupRestoreRDSStatusDTO> queue = new LinkedBlockingQueue<>(MAX_LISTENERS_LOGS);

            // Adiciona apenas até o limite
            for (BackupRestoreRDSStatusDTO log : lastLogs) {
                if (!queue.offer(log)) { // 'offer()' retorna false se não conseguir adicionar
                    break;
                }
            }

            return queue;
        });
    }

    public void unregisterListener() {
        listeners.remove(Thread.currentThread().getId());
    }

    public void addStatus(BackupRestoreRDSStatusDTO status) {
        if (lastLogs.size() >= MAX_LOGS) {
            lastLogs.poll();
        }
        lastLogs.offer(status);
        listeners.values().forEach(queue -> {
            if (queue.size() >= MAX_LISTENERS_LOGS) {
                queue.poll();
            }
            queue.offer(status);
        });
    }

    public BackupRestoreRDSStatusDTO readLog() {
        long threadId = Thread.currentThread().getId();
        LinkedBlockingQueue<BackupRestoreRDSStatusDTO> listenerLogs = listeners.get(threadId);
        if (listenerLogs == null) {
            return null;
        }
        return listenerLogs.poll();
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
