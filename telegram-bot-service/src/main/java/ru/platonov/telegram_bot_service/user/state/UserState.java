package ru.platonov.telegram_bot_service.user.state;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserState {


    private String action;
    private int step;

    public UserState(String action) {
        this.action = action;
        this.step = 0;
    }

    public void nextStep() {
        this.step++;
    }

    @Override
    public String toString() {
        return "UserState{" +
                "action='" + action + '\'' +
                ", step=" + step +
                '}';
    }
}
