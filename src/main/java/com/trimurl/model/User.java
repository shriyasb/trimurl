package com.trimurl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;
    private String password;
    private String role = "USER";
    private boolean accountDisabled = false;

    public User() {}

    public User(String email, String name, String password) {
        this.email = email; this.name = name; this.password = password; this.role = "USER";
    }

    public User(String email, String name, String password, String role) {
        this.email = email; this.name = name; this.password = password; this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isAccountDisabled() { return accountDisabled; }
    public void setAccountDisabled(boolean accountDisabled) { this.accountDisabled = accountDisabled; }
}
