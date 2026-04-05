package model;

import java.time.LocalDateTime;

public class LoginSecurityState {
    private int id;
    private String identifier;
    private String type; // "IP" or "EMAIL"
    private int currentAttempts;
    private int currentLevel;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime blockedUntil;
    private boolean mustResetPassword;

    public LoginSecurityState() {}

    public LoginSecurityState(String identifier, String type) {
        this.identifier = identifier;
        this.type = type;
        this.currentAttempts = 0;
        this.currentLevel = 1;
        this.mustResetPassword = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCurrentAttempts() { return currentAttempts; }
    public void setCurrentAttempts(int currentAttempts) { this.currentAttempts = currentAttempts; }

    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }

    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public LocalDateTime getBlockedUntil() { return blockedUntil; }
    public void setBlockedUntil(LocalDateTime blockedUntil) { this.blockedUntil = blockedUntil; }

    public boolean isMustResetPassword() { return mustResetPassword; }
    public void setMustResetPassword(boolean mustResetPassword) { this.mustResetPassword = mustResetPassword; }

    public boolean isBlocked() {
        return blockedUntil != null && blockedUntil.isAfter(LocalDateTime.now());
    }
}
