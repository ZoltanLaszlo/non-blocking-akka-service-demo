# Akka Webinar Akka demo service

## Webinar

Az előadásról készült felvétel az alábbi linken érhető el.

https://youtu.be/HITgoguM3hI

Az előadásban bemutattot Spring demo service az alábbi linken érhető el.

https://github.com/ZoltanLaszlo/blocking-spring-service-demo

## Futtatás

A demóalkalmazás futtatásához egy MS SQL Server adatbázisra van szükség, amelynek az elérési paraméterei az
application.conf fájlban találhatóak:

```hocon
url = "jdbc:sqlserver://localhost:1433;database=RXJAVA"
user = "SA"
password = "Asdfghjkl#123"
```

## Adatbázis

A szolgáltatás működéséhez az alábbi adatbázis struktúra létrehozására van szükség

```sql
CREATE
DATABASE RXJAVA COLLATE Latin1_General_CS_AS_WS;
USE
RXJAVA;

CREATE SCHEMA demo;

CREATE TABLE demo.FinancialTransaction
(
    TransactionId         CHAR(36)
        CONSTRAINT PK_FinancialTransaction
            PRIMARY KEY CLUSTERED,
    PreviousTransactionId CHAR(36) NULL,
    Data                  VARBINARY( MAX) NOT NULL
);

CREATE TABLE demo.ArchiveFinancialTransaction
(
    AccountStatementId CHAR(36) NOT NULL,
    TransactionId      CHAR(36) NOT NULL,
    TransactionNumber  INT      NOT NULL,
    CompressedData     VARBINARY( MAX) NOT NULL,
    Signature          BINARY(512) NOT NULL,
    CONSTRAINT PK_ArchiveFinancialTransaction
        PRIMARY KEY CLUSTERED (AccountStatementId, TransactionId)
);

CREATE UNIQUE INDEX AK_ArchiveFinancialTransaction_TransactionId
    ON demo.ArchiveFinancialTransaction (TransactionId);
```

## Tesztadat

Tesztadatot a Spring demó service segítségével lehet előállítani. Bővebben
lásd: https://github.com/ZoltanLaszlo/blocking-spring-service-demo

## Tesztelés

Az alábbi REST API végpont hívással indítható el a szolgáltatás által végzett kivonatolás. Értelem szerűen a HTTP
body-ban meghatározott lastTranscationId értékét ki kell cserélni egy
a [Spring demo service](https://github.com/ZoltanLaszlo/blocking-spring-service-demo) által létrehozott "
top_transactionIds.txt"-ben található UUID egyikére. (Mindegyik UUID-re csak egyszer futtatható a "számlakivonat"
generálás.)

```shell
curl --location --request POST 'http://localhost:8080/api/v1/account-service/accounts/statements' \
--header 'Content-Type: application/json' \
--data-raw '{
  "lastTransactionId": "3c3971df-7eaa-44da-8fd5-0dad16093a1d"
}'
```

A végpont OpenAPI leírója az [oas.yml](https://github.com/ZoltanLaszlo/blocking-spring-service-demo/blob/main/oas.yml)
fájlban található.
(Megjegyzés.: A lekérdező GET-es végpont nem került megvalósításra. A teszt szempontjából csak a POST-os végpont
releváns.)