# eudiw-verifier-demo

Demo av verifisering verified credentials til digital lommebok.


## Kjøre lokalt

Java 23

### Dev
Hosts-fil:
```
127.0.0.1 abr.vc.local
```
Starte med spring boot, profilen `dev` er satt opp for å kjøre på http://abr.vc.local:8080/ .

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker
Køyr kommandolinje:
```
docker compose up -build
```
Starter med profilen `docker`, og køyrer på url https://abr.vc.local:8080 

