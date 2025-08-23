package com.elpandor.hlh.common.utils;

import lombok.Data;

/**
 * Format de reponse pour les API retournant un objet
 */
@Data
public class SingleResponseData<T> {
    private boolean status;
    private T content;
    private String message;
}
