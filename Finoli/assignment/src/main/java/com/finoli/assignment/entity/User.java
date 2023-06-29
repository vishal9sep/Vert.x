package com.finoli.assignment.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  private Integer id;
  private String name;
  private String email;
  private String gender;
  private String status;
  private LocalDateTime timestamp;

  public User(String name, String email, String gender, String status, LocalDateTime timestamp){
    this.setName(name);
    this.setEmail(email);
    this.setGender(gender);
    this.setStatus(status);
    this.setTimestamp(timestamp);
  }

}
