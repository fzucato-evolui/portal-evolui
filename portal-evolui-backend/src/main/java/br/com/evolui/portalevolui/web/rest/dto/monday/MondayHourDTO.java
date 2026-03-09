package br.com.evolui.portalevolui.web.rest.dto.monday;

public class MondayHourDTO {
    private int hour;
    private int minute;

    public MondayHourDTO() {

    }

    public MondayHourDTO(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }
    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }
}
