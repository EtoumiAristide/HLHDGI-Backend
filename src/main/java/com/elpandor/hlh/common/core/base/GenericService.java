package com.elpandor.hlh.common.core.base;

import org.springframework.data.domain.Page;

import java.util.List;

public interface GenericService <E, I, D> {

    /**
     * E : Entity Class
     * I : type of Id element
     * D : DTO POJO
     */

    public D get(I id);
    public List<D> getAll();
    public Page<D> getAllPagined(Integer pageNumber, Integer size);
    public Page<D> getAllPagined(Integer pageNumber, Integer size, String sortProperty);
    public D saveOrUpdate(D element);
    public void delete(I id);
    public boolean isExist(I id);
    public E transformDTOToEntity(D element);
    public D transformEntityToDTO(E element);
}
