Allows connecting to a stream of events according to the [Server-Sent Events](../README.md) protocol, logging everything that happens.

## Compile

```bash
./../../gradlew build installDist
```

## Run

```text
$ ./build/install/sample-jvm/bin/sample-jvm -h

Usage: sample-jvm [OPTIONS] URL

Options:
  -t, --token TEXT  Authorization Token
  -h, --help        Show this message and exit

Arguments:
  URL  Endpoint URL
```

## Examples

1. Using [`dummy--server`](biowink/dummy-procedure-server):
   
   ```bash
   ./build/install/sample-jvm/bin/sample-jvm 'http://localhost:8080/procedures'
   ```
