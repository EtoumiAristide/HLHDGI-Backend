# Architecture d'impression multi-points de vente

## Principe
Outbox transactionnelle côté backend + agents desktop qui **tirent** (long-polling HTTPS sortant) les tickets de leur établissement.

```mermaid
sequenceDiagram
  participant C as Client externe
  participant B as Backend (toFacture)
  participant FNE
  participant DB as print_jobs (outbox)
  participant T as ImpressionDispatchTask
  participant A as Agent desktop (POS)
  C->>B: POST /tickets/toFacture (code établissement)
  B->>FNE: certification
  FNE-->>B: référence + lien
  B->>DB: fichiers_source + tickets/factures ; print_jobs (PENDING)
  B-->>C: réponse succès/échec (ticketsEnFileImpression)
  DB-->>T: évènement AFTER_COMMIT + tâche périodique
  T-->>A: réveil du long-poll du code établissement
  A->>B: poll (clé agent)
  B->>DB: réserve (SKIP LOCKED, bail) → DISPATCHED
  B-->>A: jobs
  A->>A: impression Jasper
  A->>B: ack PRINTED / FAILED
```

## Décisions
- **Pas d'appel du cloud vers le poste** : NAT/IP dynamique/pare-feu rendent cela fragile ; l'agent initie la connexion.
- **Outbox** : aucun ticket perdu si le poste est éteint ; reprise à la reconnexion.
- **Routage** : `print_jobs.code_etablissement` (copié de `fichiers_source`, normalisé trim+majuscules) ; l'agent est lié à un code par sa clé (hash SHA-256 en base).
- **Fiabilité** : bail (`impression.lease-seconds`), retry avec backoff (`impression.retry-backoff-seconds`, `impression.max-tentatives`), idempotence par `numero_facture` unique et journal local.
- **« Tâche d'arrière-plan »** : `@Async @TransactionalEventListener(AFTER_COMMIT)` pour la réactivité + `@Scheduled` (`impression.housekeeping-ms`) pour recycler les baux expirés.
- **Un job par facture acceptée par la FNE**, même si le lot échoue partiellement ; l'impression n'est jamais bloquante pour la réponse au client.
- **Spring Boot côté desktop** : conservé uniquement comme conteneur DI/configuration (`web-application-type=none`) ; plus de serveur web.

## Configuration backend (`impression.*`)
`lease-seconds=60`, `max-tentatives=5`, `retry-backoff-seconds=30`, `housekeeping-ms=5000`, `admin-roles=ROLE_Admin,ROLE_Super-Admin`.
Timeout du reverse proxy ≥ 60 s pour le long-polling (max 50 s).

## Limites / évolutions
- Long-polling bloquant (1 thread/agent) : pour de nombreux postes, passer à `DeferredResult`/WebSocket, ou `LISTEN/NOTIFY`.
- Avoirs (tickets négatifs) non imprimés en v1 (champ `typeDocument` prévu pour l'extension).
- Test recommandé : Testcontainers PostgreSQL pour `FOR UPDATE SKIP LOCKED`.
