package br.com.evolui.portalevolui.web.listener;

public interface ProgressStatusListener {
    void onProgress(double percentage);
    void onTotalBytes(long totalBytes);
    void onReadBytes(byte[] readBytes, int length);
}
