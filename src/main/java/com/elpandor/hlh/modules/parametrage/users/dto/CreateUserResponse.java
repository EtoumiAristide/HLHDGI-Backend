package com.elpandor.hlh.modules.parametrage.users.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserResponse implements Serializable {

    private boolean status;
    private String message;
    private Object data;
}
