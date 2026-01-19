# eudiw-verifier-demo / Digdir demo brukarstad

> [!WARNING]
> Disclaimer: Dette er ikke kode laget for produksjon, bruk er på eget ansvar.


Digdir demo brukarstad for verifisering av verified credentials til digital lommebok. Denne applikasjonen er laget for å utforske spesifikasjonen for [OpenID for Verifiable Presentations](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html), men er ikke en komplett implementasjon av hele spesifikasjonen.

Mer dokumentasjon om tenestene i digital lommebok finner du her https://docs.digdir.no/docs/lommebok/lommebok_tjenester.html. 

## Kjøre lokalt

Krav:
* Java 25
* Redis

### Maven (dev profil)
Hosts-fil:
```
127.0.0.1 abr.vc.local
```
Start redis lokalt ved å kjøre docker compose (se under).

Starte med spring boot, profilen `dev` er satt opp for å kjøre på https://abr.vc.local:8082/ .

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker (docker profil)
Dette oppsettet er pt. ikke helt i orden for lokal utvikling...

Køyr kommandolinje:
```
docker compose up --build
```
Starter med profilen `docker`, og køyrer på url https://abr.vc.local:8082 
