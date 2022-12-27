package io.proj3ct.telegramjokebot.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity(name = "usersDataTable") // This tells Hibernate to make a table out of this class
@Data
public class User {

    @Id
    private Long chatId;

    private Boolean embedeJoke;

    private String phoneNumber;

    private java.sql.Timestamp registeredAt;

    private String firstName;

    private String lastName;

    private String userName;

    private Double latitude;

    private Double longitude;

    private String bio;

    private String description;

    private String pinnedMessage;
}