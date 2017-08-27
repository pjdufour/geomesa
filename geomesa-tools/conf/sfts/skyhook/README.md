# Skyhook for GeoMesa

This directory provides GeoMesa ingest commands and converter configuration files for open data from [Skyhook](http://www.skyhookwireless.com).

Skyhook provides an OpenTIDE (Tiled Device Density Estimate) CSV for many cities around the world.

This readme describes the full process from original source data to GeoMesa ingest.

## Getting Skyhook data

The Skyhook data set can be downloaded using the provided ```download-data.sh``` script in `$GEOMESA_HOME/bin/` as such

    ./download-data.sh skyhook

Alternatively, download the OpenTIDE data from [Skyhook hosted by Carto](http://skyhook.carto.com/).  Scroll to the city, click on the "Available Data" dropdown and select a date. This download will be a single CSV file.

For more information on TIDE data from Skyhook, see the [announcement by Carto](https://twitter.com/iamwfx/status/898254032837292032).

## Ingest Commands

Check that `skyhook_tide` simple feature type is available on the GeoMesa tools classpath. This is the default case.

    geomesa env | grep skyhook_tide

If it is not, merge the contents of `reference.conf` to `$GEOMESA_HOME/conf/application.conf`, or ensure that `reference.conf` is in `$GEOMESA_HOME/conf/sfts/skyhook`.

Run the ingest. You may optionally point to a different accumulo instance using `-i` and `-z` options. See `geomesa help ingest` for more detail.

    geomesa ingest -u USERNAME -c CATALOGNAME -s skyhook_tide -C skyhook_tide dc-baltimore_maryland_2017-07-01.csv

Further be aware that any errors in ingestion will be logged to `$GEOMESA_HOME/logs`
