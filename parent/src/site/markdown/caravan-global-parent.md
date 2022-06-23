## Caravan Global Parent

Global parent for Caravan Maven artifact hierarchy. Based on [wcm.io Global Parent](https://wcm.io/tooling/maven/global-parent.html).
Defines fixed versions of Maven plugins to be used and certain general Maven settings.

[![Maven Central](https://img.shields.io/maven-central/v/io.wcm.caravan.maven/io.wcm.caravan.maven.caravan-global-parent)](https://repo1.maven.org/maven2/io/wcm/caravan/maven/io.wcm.caravan.maven.caravan-global-parent)


### Overview

The settings in this global parent POM cover:

* Set compiler setting for Jave 8
* Configure Retrolambda Plugin to transform bytecode for Java 7
* Configure Animal Sniffer Plugin to ensure no Java 8 Runtime Libraries are used
* Define SCR and OSGi dependencies and plugins
