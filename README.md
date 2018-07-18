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

## Processor properties

__FlowFile Format__
Specify the format of the incoming FlowFile. If AVRO, output is automatically Snappy compressed.

__Avro Schema__
Specify the schema if the FlowFile format is Avro.

__Invalid Dates__
JSON Object of key/value pairs with name of field in FlowFile as key and type of date as value. For example: {"my_date_field": "MM/dd/yyyy"}

__Timezone__
The originating timezone of the date fields in the FlowFile. Short or standard IDs accepted (i.e. 'CST' or 'America/Chicago')

### Notes

- The incoming FlowFile is expected to be one JSON per line.
- If the `Invalid Dates` property is not set, the processor automatically sends the FlowFile to the `bypass` relationship.
- Avro is always Snappy compressed on output.
- This processor uses a [custom Avro library](https://github.com/zolyfarkas/avro) in order to handle Avro's union types. Until [this issue](https://issues.apache.org/jira/browse/AVRO-1582) is resolved, it will continue to use the custom library.

### TODO

- Use drop-down (with custom option) for timezone
- Allow choice of Avro compression (Snappy, bzip2, etc.)
- Infer Avro schema if not passed in
- Better unit tests for Avro
