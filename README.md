# nifi-standardize-date-bundle

NiFi processor to standardize date fields in a FlowFile.

## Deploy Bundle

Clone this repository

```shell
git clone https://github.com/1904labs/nifi-standardize-date-bundle
```

Build the bundle

```shell
cd nifi-standardize-date-bundle
mvn clean install
```

Copy Nar file to $NIFI_HOME/lib

```shell
cp nifi-standardize-date-bundle/target/nifi-standardize-date-nar-$version.nar $NIFI_HOME/lib/
```

Start/Restart Nifi

```shell
$NIFI_HOME/bin/nifi.sh start
```
