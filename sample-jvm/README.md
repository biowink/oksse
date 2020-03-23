Sample app to test the SSE server, it allows connecting to a stream of events according to the [Server-Sent Events](../README.md) protocol and
logs everything that happens.

Use any dummy SSE server or the one provided in the project to test the sample app

## Compile

```bash
./../../gradlew build installDist
```

## Run

```text
$ ./build/install/sample-jvm/bin/sample-jvm -h

Usage: sample-jvm [OPTIONS] URL

Options:
  -t, --token TEXT  Authorization Token [Optional]
  -h, --help        Show this message and exit

Arguments:
  URL  Endpoint URL
```
