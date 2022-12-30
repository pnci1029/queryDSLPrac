package com.example.study.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {

    private String name;

    public UserDto(String name, int age) {
        this.name = name;
        this.age = age;
    }

    private int age;

}