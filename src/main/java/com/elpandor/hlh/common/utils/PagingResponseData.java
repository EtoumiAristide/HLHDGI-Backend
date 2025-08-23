package com.elpandor.hlh.common.utils;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Format de reponse pour les API retournant une liste paginée d'objets
 */
@Data
@Builder
public class PagingResponseData<T> {
    private boolean status;
    private List<T> content;
    private String message;
    private int current_page;
    private int total_items;
    private int total_pages;
    private int page_size;
}
