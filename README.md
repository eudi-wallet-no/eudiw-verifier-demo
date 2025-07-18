# eudiw-verifier-demo

Demo av verifisering verified credentials til digital lommebok.


## Kjøre lokalt

Java 24
Redis

### Dev
Hosts-fil:
```
127.0.0.1 abr.vc.local
```
Start redis lokalt ved å kjøre docker compose (se under).

Starte med spring boot, profilen `dev` er satt opp for å kjøre på https://abr.vc.local:8082/ .

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker
Dette oppsettet er pt ikke helt i orden for lokal utvikling...

Køyr kommandolinje:
```
docker compose up --build
```
Starter med profilen `docker`, og køyrer på url https://abr.vc.local:8082 
