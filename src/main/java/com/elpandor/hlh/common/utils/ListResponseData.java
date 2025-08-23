package com.elpandor.hlh.common.utils;

import lombok.Data;

import java.util.List;

/**
 * Format de reponse pour les API retournant une liste d'objet
 */
@Data
public class ListResponseData<T> {
    private boolean status;
    private List<T> content;
    private String message;
    private int total_items;
}
