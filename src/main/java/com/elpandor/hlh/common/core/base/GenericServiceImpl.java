package com.elpandor.hlh.common.core.base;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public abstract class GenericServiceImpl<E, I, D> implements GenericService<E, I, D> {

    protected JpaRepository<E, I> repository;

    private Logger log = LoggerFactory.getLogger(GenericServiceImpl.class);

    public GenericServiceImpl(JpaRepository<E, I> repository) {
        this.repository = repository;
    }

    @Override
    public D get(I id) {
        log.trace("<== Inside generic get method ==> ");

        Optional<E> element = repository.findById(id);
        if (element.isPresent()) {
            log.trace("<== Got the response from dao and received an object ==>");
            return this.transformEntityToDTO(element.get());
        }

        log.info("<== No element is found for the passed id ==>");
        return null;
    }

    @Override
    public List<D> getAll() {
        log.trace("<== Inside getAll() of generic service ==>");

        List<E> list = repository.findAll();

        log.trace("<== Got the response from the repository from getALL  ==>");
        return list.stream().map(this::transformEntityToDTO).collect(Collectors.toList());
    }

    @Override
    public Page<D> getAllPagined(Integer pageNumber, Integer size) {
        log.trace("<== Inside getAll() of generic service ==>");


        pageNumber = pageNumber != null ? pageNumber : 0;
        size = size != null ? size : 10;
        Pageable page = PageRequest.of(pageNumber, size, Sort.by("id").ascending());
        Page<D> pagination = repository.findAll(page).map(this::transformEntityToDTO);

        log.trace("<== Got the response from the repository from getALL  ==>");
        return pagination;
    }

    @Override
    public Page<D> getAllPagined(Integer pageNumber, Integer size, String sortProperty) {
        log.trace("<== Inside getAll() of generic service ==>");


        pageNumber = pageNumber != null ? pageNumber : 0;
        size = size != null ? size : 10;
        Pageable page = PageRequest.of(pageNumber, size, Sort.by(sortProperty).ascending());
        Page<D> pagination = repository.findAll(page).map(this::transformEntityToDTO);

        log.trace("<== Got the response from the repository from getALL  ==>");
        return pagination;
    }

    @Override
    public D saveOrUpdate(D element) {
        log.trace("<== Inside sabeOrOpdate() of generic service ==>");
        return this.transformEntityToDTO(repository.save(transformDTOToEntity(element)));
    }

    @Override
    public void delete(I id) {
        log.trace("<== Inside delete() of generic service ==>");

        repository.deleteById(id);
    }

    @Override
    public boolean isExist(I id) {
        log.trace("<== Inside isExist() of generic service ==>");

        return repository.findById(id).isPresent();
    }

    @Override
    public E transformDTOToEntity(D element) {
        return null;
    }

    @Override
    public D transformEntityToDTO(E element) {
        return null;
    }
}
