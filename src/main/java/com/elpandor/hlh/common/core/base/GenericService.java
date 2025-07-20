package com.elpandor.hlh.common.core.base;

import java.util.List;

public interface GenericService <E, I, D> {

    /**
     * E : Entity Class
     * I : type of Id element
     * D : DTO POJO
     */

    public D get(I id);
    public List<D> getAll(Integer pageNumber, Integer size);
    public D saveOrUpdate(D element);
    public void delete(I id);
    public boolean isExist(I id);
    public E transformDTOToEntity(D element);
    public D transformEntityToDTO(E element);
}
