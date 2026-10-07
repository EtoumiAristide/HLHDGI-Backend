package com.elpandor.hlh.modules.impressionzino.application;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registre des agents en attente (long-polling), par code établissement.
 * <p>
 * Un agent sans job à traiter s'enregistre ici et dort au plus quelques dizaines de secondes ;
 * {@link #signaler(String)} le réveille immédiatement quand un job arrive. Le signal ne porte aucune
 * donnée : l'agent réveillé réserve lui-même ses jobs en base, ce qui reste correct même si le signal
 * est perdu (le prochain poll ou la tâche de maintenance retrouve les jobs).
 */
@Component
public class PrintSignalRegistry {

    private final ConcurrentMap<String, Set<CompletableFuture<Void>>> attentes = new ConcurrentHashMap<>();

    public CompletableFuture<Void> enregistrer(String codeEtablissement) {
        CompletableFuture<Void> signal = new CompletableFuture<>();
        attentes.computeIfAbsent(codeEtablissement, k -> ConcurrentHashMap.newKeySet()).add(signal);
        return signal;
    }

    public void retirer(String codeEtablissement, CompletableFuture<Void> signal) {
        Set<CompletableFuture<Void>> signaux = attentes.get(codeEtablissement);
        if (signaux != null) {
            signaux.remove(signal);
        }
    }

    public void signaler(String codeEtablissement) {
        Set<CompletableFuture<Void>> signaux = attentes.get(codeEtablissement);
        if (signaux != null) {
            signaux.forEach(s -> s.complete(null));
        }
    }
}
