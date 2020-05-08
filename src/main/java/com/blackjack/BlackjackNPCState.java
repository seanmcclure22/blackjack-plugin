package com.blackjack;

import lombok.Getter;

public enum BlackjackNPCState {
    AWAKEN("Awaken"),
    KNOCKED_OUT("Knocked Out"),
    AGGRESSIVE("Aggressive");

    @Getter
    public String displayName;

    private BlackjackNPCState(String displayName)
    {
        this.displayName = displayName;
    }
}
