# Genie

A Maven plugin for Genie -- a tool to automatically generate inline tests for target statements.

## Prerequisites

#### Install `inlinetest` through Maven

```bash
git clone git@github.com:EngineeringSoftware/inlinetest.git
cd inlinetest/java
mvn clean install
cd -
```

#### Install `exli/raninline` through Maven

```bash
git clone git@github.com:EngineeringSoftware/exli.git
cd exli/java/raninline
mvn clean install
cd -
```

#### Install `universalmutator`

Install universalmutator with either pip:

```bash
python3 -m pip install universalmutator
```

or any other methods.

## Usage

### Installation

Install `genie-maven-plugin` with:

```bash
mvn clean install
```

### POM

Add `inlinetest` and `exli/raninline` as a dependency under your Maven `pom.xml`'s `<dependencies>`:

```xml
<dependency>
	<groupId>org.inlinetest</groupId>
  <artifactId>inlinetest</artifactId>
  <version>1.0</version>
</dependency>
<dependency>
  <groupId>org.raninline</groupId>
  <artifactId>raninline</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Add this to your `pom.xml`, under `<build>`, under `<plugins>`:

```xml
<plugin>
  <artifactId>genie-maven-plugin</artifactId>
  <groupId>org.genie</groupId>
  <version>1.0-SNAPSHOT</version>
</plugin>
```

This is to include both Genie Maven plugin and JaCoCo Maven plugin.

### Sample run

This is an example that runs the `extractor` functionality on `commons-lang`:

```bash
git clone https://github.com/apache/commons-lang
(
	cd commons-lang
	git checkout -f 08988a2c9029b1625115b3e3c6d30cb0fd1de57a
	# Edit pom.xml with the right plugin declaration from the previous section.
	mvn clean genie:extractor -DfilePath="src/main/java/org/apache/commons/lang3/StringEscapeUtils.java" -DlineNumbers=93 -DlogVariables=false
	cat ./src/main/java/org/apache/commons/lang3/StringEscapeUtils_93.java # To see the extracted target statement.
)
```
